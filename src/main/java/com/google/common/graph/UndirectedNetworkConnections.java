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

package com.google.common.graph;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

/**
 * An implementation of {@link NetworkConnections} for undirected networks.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
final class UndirectedNetworkConnections<N, E> extends AbstractUndirectedNetworkConnections<N, E> {

    UndirectedNetworkConnections(Map<E, N> incidentEdgeMap) {
        super(incidentEdgeMap);
    }

    static <N, E> UndirectedNetworkConnections<N, E> of() {
        return new UndirectedNetworkConnections<>(new LinkedHashMap<>());
    }

    static <N, E> UndirectedNetworkConnections<N, E> ofImmutable(Map<E, N> incidentEdges) {
        return new UndirectedNetworkConnections<>(Collections.unmodifiableMap(new LinkedHashMap<>(incidentEdges)));
    }

    @Override
    public Set<N> adjacentNodes() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(incidentEdgeMap.values()));
    }

    @Override
    public Set<E> edgesConnecting(N node) {
        Map<N, E> inverse = incidentEdgeMap.entrySet().stream()
                .collect(collectingAndThen(toMap(Map.Entry::getValue, Map.Entry::getKey), LinkedHashMap::new));
        return new EdgesConnecting<>(inverse, node);
    }
}
