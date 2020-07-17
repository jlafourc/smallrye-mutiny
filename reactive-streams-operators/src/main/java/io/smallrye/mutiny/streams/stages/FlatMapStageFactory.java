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
package io.smallrye.mutiny.streams.stages;

import java.util.Objects;
import java.util.function.Function;

import org.eclipse.microprofile.reactive.streams.operators.spi.Graph;
import org.eclipse.microprofile.reactive.streams.operators.spi.Stage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.streams.Engine;
import io.smallrye.mutiny.streams.operators.ProcessingStage;
import io.smallrye.mutiny.streams.operators.ProcessingStageFactory;
import io.smallrye.mutiny.streams.utils.Casts;

/**
 * Implementation of the {@link Stage.FlatMap} stage. Be aware it behaves as a RX `concatMap`.
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
 */
public class FlatMapStageFactory implements ProcessingStageFactory<Stage.FlatMap> {

    @Override
    public <I, O> ProcessingStage<I, O> create(Engine engine, Stage.FlatMap stage) {
        Function<I, Graph> mapper = Casts.cast(stage.getMapper());
        return new FlatMapStage<>(engine, mapper);
    }

    private static class FlatMapStage<I, O> implements ProcessingStage<I, O> {
        private final Engine engine;
        private final Function<I, Graph> mapper;

        private FlatMapStage(Engine engine, Function<I, Graph> mapper) {
            this.mapper = Objects.requireNonNull(mapper);
            this.engine = engine;
        }

        @Override
        public Multi<O> apply(Multi<I> source) {
            return source
                    .onItem().transformToMultiAndConcatenate(item -> {
                        Graph graph = mapper.apply(item);
                        return engine.buildPublisher(Objects.requireNonNull(graph));
                    });
        }
    }
}
