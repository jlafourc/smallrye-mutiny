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
package io.smallrye.mutiny.groups;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Functions;
import io.smallrye.mutiny.tuples.Tuple3;
import io.smallrye.mutiny.tuples.Tuples;

public class UniAndGroup3<T1, T2, T3> extends UniAndGroupIterable<T1> {

    public UniAndGroup3(Uni<? extends T1> source, Uni<? extends T2> o1, Uni<? extends T3> o2) {
        super(source, Arrays.asList(o1, o2));
    }

    public UniAndGroup3<T1, T2, T3> collectFailures() {
        super.collectFailures();
        return this;
    }

    public Uni<Tuple3<T1, T2, T3>> asTuple() {
        return combinedWith(Tuple3::of);
    }

    @SuppressWarnings("unchecked")
    public <O> Uni<O> combinedWith(Functions.Function3<T1, T2, T3, O> combinator) {
        Function<List<?>, O> function = list -> {
            Tuples.ensureArity(list, 3);
            T1 item1 = (T1) list.get(0);
            T2 item2 = (T2) list.get(1);
            T3 item3 = (T3) list.get(2);
            return combinator.apply(item1, item2, item3);
        };
        return super.combinedWith(function);
    }

}
