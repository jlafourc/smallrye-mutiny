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

import org.testng.annotations.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.test.MultiAssertSubscriber;

public class MultiCastTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThatClassCannotBeNull() {
        Multi.createFrom().item(1)
                .onItem().castTo(null);
    }

    @Test
    public void testCastThatWorks() {
        Multi.createFrom().item(1)
                .onItem().castTo(Number.class)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertCompletedSuccessfully()
                .assertReceived(1);
    }

    @Test
    public void testCastThatDoesNotWork() {
        Multi.createFrom().item(1)
                .onItem().castTo(String.class)
                .subscribe().withSubscriber(MultiAssertSubscriber.create(1))
                .assertHasFailedWith(ClassCastException.class, "String");
    }
}
