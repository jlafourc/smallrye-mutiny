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

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.validate;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.multi.builders.IntervalMulti;

public class MultiTimePeriod {

    private Duration initialDelay;
    private ScheduledExecutorService executor = Infrastructure.getDefaultWorkerPool();

    public MultiTimePeriod startingAfter(Duration duration) {
        this.initialDelay = validate(duration, "duration");
        return this;
    }

    public MultiTimePeriod onExecutor(ScheduledExecutorService executor) {
        this.executor = nonNull(executor, "executor");
        return this;
    }

    public Multi<Long> every(Duration duration) {
        validate(duration, "duration");
        if (initialDelay != null) {
            return Infrastructure.onMultiCreation(new IntervalMulti(initialDelay, duration, executor));
        } else {
            return Infrastructure.onMultiCreation(new IntervalMulti(duration, executor));
        }
    }

}
