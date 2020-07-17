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
package io.smallrye.mutiny.operators.uni.builders;

import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.operators.UniSerializedSubscriber;

/**
 * Specialized {@link io.smallrye.mutiny.Uni} implementation for the case where the item is known.
 * The item can be {@code null}.
 *
 * @param <T> the type of the item
 */
public class KnownItemUni<T> extends AbstractUni<T> {

    private final T item;

    public KnownItemUni(T item) {
        this.item = item;
    }

    @Override
    protected void subscribing(UniSerializedSubscriber<? super T> subscriber) {
        // No need to track cancellation, it's done by the serialized subscriber downstream.
        subscriber.onSubscribe(EmptyUniSubscription.CANCELLED);
        subscriber.onItem(item);
    }
}
