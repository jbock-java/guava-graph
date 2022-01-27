/*
 * Copyright (C) 2016 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jbock.common.graph;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

/**
 * An implementation of {@link NetworkConnections} for directed networks.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
final class DirectedNetworkConnections<N, E> extends AbstractDirectedNetworkConnections<N, E> {

    DirectedNetworkConnections(Map<E, N> inEdgeMap, Map<E, N> outEdgeMap, int selfLoopCount) {
        super(inEdgeMap, outEdgeMap, selfLoopCount);
    }

    static <N, E> DirectedNetworkConnections<N, E> of() {
        return new DirectedNetworkConnections<>(
                new LinkedHashMap<>(), new LinkedHashMap<>(), 0);
    }

    static <N, E> DirectedNetworkConnections<N, E> ofImmutable(
            Map<E, N> inEdges, Map<E, N> outEdges, int selfLoopCount) {
        return new DirectedNetworkConnections<>(
                Collections.unmodifiableMap(new LinkedHashMap<>(inEdges)),
                Collections.unmodifiableMap(new LinkedHashMap<>(outEdges)), selfLoopCount);
    }

    @Override
    public Set<N> predecessors() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(inEdgeMap.values()));
    }

    @Override
    public Set<N> successors() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(outEdgeMap.values()));
    }

    @Override
    public Set<E> edgesConnecting(N node) {
        Map<N, E> inverse = outEdgeMap.entrySet().stream()
                .collect(collectingAndThen(toMap(Map.Entry::getValue, Map.Entry::getKey), LinkedHashMap::new));
        return new EdgesConnecting<>(inverse, node);
    }
}
