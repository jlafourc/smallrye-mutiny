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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.mockito.internal.stubbing.answers.ThrowsException;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.testng.annotations.Test;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.helpers.Subscriptions;

@SuppressWarnings("unchecked")
public class SafeSubscriberTest {

    @Test
    public void testThatDownstreamFailureInOnSubscribeCancelsTheSubscription() {
        Subscriber<Integer> subscriber = mock(Subscriber.class);
        doAnswer(new ThrowsException(new IllegalStateException("boom"))).when(subscriber).onSubscribe(any());
        Subscription subscription = mock(Subscription.class);

        SafeSubscriber<Integer> safe = new SafeSubscriber<>(subscriber);
        safe.onSubscribe(subscription);
        verify(subscription, times(1)).cancel();
        assertThat(safe.isDone()).isTrue();
    }

    @Test
    public void testDownstreamFailureInOnSubscribeFollowedByACancellationFailure() {
        Subscriber<Integer> subscriber = mock(Subscriber.class);
        doAnswer(new ThrowsException(new IllegalStateException("boom"))).when(subscriber).onSubscribe(any());
        Subscription subscription = mock(Subscription.class);
        doAnswer(new ThrowsException(new NullPointerException())).when(subscription).cancel();

        SafeSubscriber<Integer> safe = new SafeSubscriber<>(subscriber);
        safe.onSubscribe(subscription);
        verify(subscription, times(1)).cancel();
        assertThat(safe.isDone()).isTrue();
    }

    @Test
    public void testThatOnSubscriberCanOnlyBeCalledOnce() {
        // the second subscription must be cancelled.
        Subscriber<Integer> subscriber = mock(Subscriber.class);
        Subscription subscription1 = mock(Subscription.class);
        Subscription subscription2 = mock(Subscription.class);

        SafeSubscriber<Integer> safe = new SafeSubscriber<>(subscriber);
        safe.onSubscribe(subscription1);
        verify(subscription1, never()).cancel();

        assertThat(safe.isDone()).isFalse();

        safe.onSubscribe(subscription2);
        verify(subscription1, never()).cancel();
        verify(subscription2, times(1)).cancel();

        assertThat(safe.isDone()).isFalse();
    }

    @Test
    public void testThatOnceDoneOnNextIsNoop() {
        Subscriber<Integer> subscriber = mock(Subscriber.class);
        Subscription subscription = mock(Subscription.class);

        SafeSubscriber<Integer> safe = new SafeSubscriber<>(subscriber);

        safe.onSubscribe(subscription);
        verify(subscriber, times(1)).onSubscribe(safe);

        safe.onNext(1);
        verify(subscriber, times(1)).onNext(1);

        IOException boom = new IOException("boom");
        safe.onError(boom);
        verify(subscriber, times(1)).onError(boom);

        assertThat(safe.isDone()).isTrue();

        safe.onNext(1);
        verify(subscriber, times(1)).onNext(1); // only once

        safe.onError(boom);
        verify(subscriber, times(1)).onError(boom); // only once
    }

    @Test
    public void testThatNullItemsAreRejected() {
        Subscriber<String> subscriber = mock(Subscriber.class);
        Subscription subscription = mock(Subscription.class);

        SafeSubscriber<String> safe = new SafeSubscriber<>(subscriber);

        safe.onSubscribe(subscription);
        verify(subscriber, times(1)).onSubscribe(safe);

        safe.onNext("hello");
        verify(subscriber, times(1)).onNext("hello");

        assertThatThrownBy(() -> safe.onNext(null)).isInstanceOf(NullPointerException.class);
        //noinspection ConstantConditions
        verify(subscriber, never()).onNext(null);

        assertThat(safe.isDone()).isTrue();

        verify(subscriber, times(1)).onError(any(NullPointerException.class));
    }

    @Test
    public void testThatOnNextWithoutSubscriptionIsAProtocolViolation() {
        Subscriber<String> subscriber = mock(Subscriber.class);

        SafeSubscriber<String> safe = new SafeSubscriber<>(subscriber);

        safe.onNext("hello");
        verify(subscriber, times(0)).onNext("hello");
        verify(subscriber, times(1)).onSubscribe(any(Subscriptions.EmptySubscription.class));
        verify(subscriber, times(1)).onError(any(NullPointerException.class));
    }

    @Test
    public void testOnNextThrowingException() {
        Subscriber<Integer> subscriber = mock(Subscriber.class);
        IllegalArgumentException boom = new IllegalArgumentException("boom");
        doAnswer(new ThrowsException(boom)).when(subscriber).onNext(2);
        Subscription subscription = mock(Subscription.class);
        SafeSubscriber<Integer> safe = new SafeSubscriber<>(subscriber);

        safe.onSubscribe(subscription);
        verify(subscriber, times(1)).onSubscribe(safe);

        safe.onNext(1);
        verify(subscriber, times(1)).onNext(1);

        // inject the failing item
        safe.onNext(2);
        verify(subscriber, times(1)).onNext(2);
        verify(subscriber, times(1)).onError(boom);
        assertThat(safe.isDone()).isTrue();
    }

    @Test
    public void testOnNextThrowingExceptionFollowedByCancellationFailure() {
        Subscriber<Integer> subscriber = mock(Subscriber.class);
        IllegalArgumentException boom = new IllegalArgumentException("boom");
        doAnswer(new ThrowsException(boom)).when(subscriber).onNext(2);
        Subscription subscription = mock(Subscription.class);
        doAnswer(new ThrowsException(new NullPointerException())).when(subscription).cancel();
        SafeSubscriber<Integer> safe = new SafeSubscriber<>(subscriber);

        safe.onSubscribe(subscription);
        verify(subscriber, times(1)).onSubscribe(safe);

        safe.onNext(1);
        verify(subscriber, times(1)).onNext(1);

        // inject the failing item
        safe.onNext(2);
        verify(subscriber, times(1)).onNext(2);
        verify(subscriber, times(1)).onError(any(CompositeException.class)); // boom + NPE
        assertThat(safe.isDone()).isTrue();
    }

    @Test
    public void testThatOnErrorWithoutSubscriptionIsAProtocolViolation() {
        Subscriber<String> subscriber = mock(Subscriber.class);

        SafeSubscriber<String> safe = new SafeSubscriber<>(subscriber);

        safe.onError(new IllegalArgumentException("boom"));
        verify(subscriber, times(1)).onError(any(CompositeException.class)); // boom + NPE (no upstream)
        verify(subscriber, times(1)).onSubscribe(any(Subscriptions.EmptySubscription.class));
    }

    @Test
    public void testThatOnErrorWithoutSubscriptionFollowedBySubscriptionFailureIsAProtocolViolationButCannotBeReportedDownstream() {
        Subscriber<String> subscriber = mock(Subscriber.class);
        doAnswer(new ThrowsException(new IllegalStateException("boom"))).when(subscriber).onSubscribe(any());

        SafeSubscriber<String> safe = new SafeSubscriber<>(subscriber);

        safe.onError(new IllegalArgumentException("boom"));
        verify(subscriber, times(0)).onError(any(Exception.class));
        verify(subscriber, times(1)).onSubscribe(any(Subscriptions.EmptySubscription.class)); // failing
        assertThat(safe.isDone()).isTrue();
    }

    @Test
    public void testThatNullFailuresAreRejected() {
        Subscriber<String> subscriber = mock(Subscriber.class);
        Subscription subscription = mock(Subscription.class);

        SafeSubscriber<String> safe = new SafeSubscriber<>(subscriber);

        safe.onSubscribe(subscription);
        verify(subscriber, times(1)).onSubscribe(safe);

        assertThatThrownBy(() -> safe.onError(null)).isInstanceOf(NullPointerException.class);
        verify(subscriber, times(0)).onError(any(Exception.class));
    }

    @Test
    public void testThatDownstreamFailuresAreHandledInOnError() {
        Subscriber<String> subscriber = mock(Subscriber.class);
        doAnswer(new ThrowsException(new IllegalStateException("boom"))).when(subscriber).onError(any());

        Subscription subscription = mock(Subscription.class);

        SafeSubscriber<String> safe = new SafeSubscriber<>(subscriber);

        safe.onSubscribe(subscription);
        verify(subscriber, times(1)).onSubscribe(safe);

        Exception boom = new Exception("boom");
        safe.onError(boom);
        // called
        verify(subscriber, times(1)).onError(boom);
        assertThat(safe.isDone()).isTrue();
    }

    @Test
    public void testOnErrorOnceDoneAreNoop() {
        Subscriber<String> subscriber = mock(Subscriber.class);

        Subscription subscription = mock(Subscription.class);

        SafeSubscriber<String> safe = new SafeSubscriber<>(subscriber);

        safe.onSubscribe(subscription);
        verify(subscriber, times(1)).onSubscribe(safe);

        Exception boom = new Exception("boom");
        safe.onError(boom);
        verify(subscriber, times(1)).onError(boom);
        assertThat(safe.isDone()).isTrue();

        safe.onError(boom);
        verify(subscriber, times(1)).onError(boom); // called only once
    }

    @Test
    public void testOnErrorOnceDoneWithCompletionAreNoop() {
        Subscriber<String> subscriber = mock(Subscriber.class);

        Subscription subscription = mock(Subscription.class);

        SafeSubscriber<String> safe = new SafeSubscriber<>(subscriber);

        safe.onSubscribe(subscription);
        verify(subscriber, times(1)).onSubscribe(safe);

        safe.onComplete();
        verify(subscriber, times(1)).onComplete();
        assertThat(safe.isDone()).isTrue();

        safe.onError(new Exception("boom"));
        verify(subscriber, times(0)).onError(any()); // not called
    }

    @Test
    public void testOnCompleteOnceDoneWithCompletionAreNoop() {
        Subscriber<String> subscriber = mock(Subscriber.class);

        Subscription subscription = mock(Subscription.class);

        SafeSubscriber<String> safe = new SafeSubscriber<>(subscriber);

        safe.onSubscribe(subscription);
        verify(subscriber, times(1)).onSubscribe(safe);

        safe.onComplete();
        verify(subscriber, times(1)).onComplete();
        assertThat(safe.isDone()).isTrue();

        safe.onComplete();
        verify(subscriber, times(1)).onComplete(); // called only once
    }

    @Test
    public void testOnCompleteOnceDoneAreNoop() {
        Subscriber<String> subscriber = mock(Subscriber.class);

        Subscription subscription = mock(Subscription.class);

        SafeSubscriber<String> safe = new SafeSubscriber<>(subscriber);

        safe.onSubscribe(subscription);
        verify(subscriber, times(1)).onSubscribe(safe);

        Exception boom = new Exception("boom");
        safe.onError(boom);
        verify(subscriber, times(1)).onError(boom);
        assertThat(safe.isDone()).isTrue();

        safe.onComplete();
        verify(subscriber, times(0)).onComplete(); // not called
    }

    @Test
    public void testThatOnCompleteWithoutSubscriptionIsAProtocolViolation() {
        Subscriber<String> subscriber = mock(Subscriber.class);

        SafeSubscriber<String> safe = new SafeSubscriber<>(subscriber);

        safe.onComplete();
        verify(subscriber, times(1)).onError(any(NullPointerException.class)); // NPE (no upstream)
        verify(subscriber, times(1)).onSubscribe(any(Subscriptions.EmptySubscription.class));
        assertThat(safe.isDone()).isTrue();
    }

    @Test
    public void testFailureInDownstreamOnComplete() {
        Subscriber<Integer> subscriber = mock(Subscriber.class);
        IllegalArgumentException boom = new IllegalArgumentException("boom");
        doAnswer(new ThrowsException(boom)).when(subscriber).onComplete();
        Subscription subscription = mock(Subscription.class);
        SafeSubscriber<Integer> safe = new SafeSubscriber<>(subscriber);

        safe.onSubscribe(subscription);
        verify(subscriber, times(1)).onSubscribe(safe);

        safe.onNext(1);
        verify(subscriber, times(1)).onNext(1);

        safe.onComplete();
        verify(subscriber, times(1)).onComplete();
        verify(subscriber, times(0)).onError(any()); // cannot report.
        assertThat(safe.isDone()).isTrue();
    }

    @Test
    public void testWhenUpstreamCancellationFails() {
        Subscriber<Integer> subscriber = mock(Subscriber.class);
        Subscription subscription = mock(Subscription.class);
        doAnswer(new ThrowsException(new IllegalStateException("boom"))).when(subscription).cancel();
        SafeSubscriber<Integer> safe = new SafeSubscriber<>(subscriber);

        safe.onSubscribe(subscription);
        verify(subscriber, times(1)).onSubscribe(safe);

        safe.cancel();
    }

    @Test
    public void testWhenUpstreamRequestFails() {
        Subscriber<Integer> subscriber = mock(Subscriber.class);
        Subscription subscription = mock(Subscription.class);
        doAnswer(new ThrowsException(new IllegalStateException("boom"))).when(subscription).request(anyLong());
        SafeSubscriber<Integer> safe = new SafeSubscriber<>(subscriber);

        safe.onSubscribe(subscription);
        verify(subscriber, times(1)).onSubscribe(safe);

        safe.request(25L);
    }

}
