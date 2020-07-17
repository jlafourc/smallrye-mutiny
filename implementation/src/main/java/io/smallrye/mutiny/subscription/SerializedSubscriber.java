/*
 * Copyright (c) 2019-2020 Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.smallrye.mutiny.subscription;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Subscriber that makes sure signals are delivered sequentially in case the onNext, onError or onComplete methods are
 * called concurrently.
 * <p>
 * Class copied from Project Reactor.
 *
 * @param <T> the type of items
 */
public final class SerializedSubscriber<T> implements Subscription, MultiSubscriber<T> {

    private final Subscriber<? super T> downstream;

    private boolean emitting;

    private boolean missed;

    private volatile boolean done;

    private volatile boolean cancelled;

    private LinkedArrayNode<T> head;

    private LinkedArrayNode<T> tail;

    private Throwable failure;

    private AtomicReference<Subscription> upstream = new AtomicReference<>();

    public SerializedSubscriber(Subscriber<? super T> downstream) {
        this.downstream = downstream;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (upstream.compareAndSet(null, s)) {
            downstream.onSubscribe(this);
        } else {
            s.cancel();
        }
    }

    @Override
    public void onItem(T t) {
        if (cancelled || done) {
            return;
        }

        synchronized (this) {
            if (cancelled || done) {
                return;
            }

            if (emitting) {
                serAdd(t);
                missed = true;
                return;
            }

            emitting = true;
        }

        downstream.onNext(t);

        serDrainLoop(downstream);
    }

    @Override
    public void onFailure(Throwable t) {
        if (cancelled || done) {
            return;
        }

        synchronized (this) {
            if (cancelled || done) {
                return;
            }

            done = true;
            failure = t;

            if (emitting) {
                missed = true;
                return;
            }
        }

        downstream.onError(t);
    }

    @Override
    public void onCompletion() {
        if (cancelled || done) {
            return;
        }

        synchronized (this) {
            if (cancelled || done) {
                return;
            }

            done = true;

            if (emitting) {
                missed = true;
                return;
            }
        }

        downstream.onComplete();
    }

    @Override
    public void request(long n) {
        upstream.get().request(n);
    }

    @Override
    public void cancel() {
        cancelled = true;
        upstream.get().cancel();
    }

    void serAdd(T value) {
        LinkedArrayNode<T> t = tail;

        if (t == null) {
            t = new LinkedArrayNode<>(value);

            head = t;
            tail = t;
        } else {
            if (t.count == LinkedArrayNode.DEFAULT_CAPACITY) {
                LinkedArrayNode<T> n = new LinkedArrayNode<>(value);

                t.next = n;
                tail = n;
            } else {
                t.array[t.count++] = value;
            }
        }
    }

    void serDrainLoop(Subscriber<? super T> actual) {
        for (;;) {

            if (cancelled) {
                return;
            }

            boolean d;
            Throwable e;
            LinkedArrayNode<T> n;

            synchronized (this) {
                if (cancelled) {
                    return;
                }

                if (!missed) {
                    emitting = false;
                    return;
                }

                missed = false;

                d = done;
                e = failure;
                n = head;

                head = null;
                tail = null;
            }

            while (n != null) {

                T[] arr = n.array;
                int c = n.count;

                for (int i = 0; i < c; i++) {

                    if (cancelled) {
                        return;
                    }

                    actual.onNext(arr[i]);
                }

                n = n.next;
            }

            if (cancelled) {
                return;
            }

            if (e != null) {
                actual.onError(e);
                return;
            } else if (d) {
                actual.onComplete();
                return;
            }
        }
    }

    /**
     * Node in a linked array list that is only appended.
     *
     * @param <T> the value type
     */
    static final class LinkedArrayNode<T> {

        static final int DEFAULT_CAPACITY = 16;

        final T[] array;
        int count;

        LinkedArrayNode<T> next;

        @SuppressWarnings("unchecked")
        LinkedArrayNode(T value) {
            array = (T[]) new Object[DEFAULT_CAPACITY];
            array[0] = value;
            count = 1;
        }
    }
}
