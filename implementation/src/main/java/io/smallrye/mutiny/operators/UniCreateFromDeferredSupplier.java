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
package io.smallrye.mutiny.operators;

import static io.smallrye.mutiny.helpers.EmptyUniSubscription.CANCELLED;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.ParameterValidation;

public class UniCreateFromDeferredSupplier<T> extends UniOperator<Void, T> {
    private final Supplier<? extends Uni<? extends T>> supplier;

    public UniCreateFromDeferredSupplier(Supplier<? extends Uni<? extends T>> supplier) {
        super(null);
        this.supplier = supplier; // Already checked
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        nonNull(subscriber, "subscriber");
        Uni<? extends T> uni;
        try {
            uni = supplier.get();
        } catch (Throwable e) {
            subscriber.onSubscribe(CANCELLED);
            subscriber.onFailure(e);
            return;
        }

        if (uni == null) {
            subscriber.onSubscribe(CANCELLED);
            subscriber.onFailure(new NullPointerException(ParameterValidation.SUPPLIER_PRODUCED_NULL));
        } else {
            AbstractUni.subscribe(uni, subscriber);
        }
    }
}
