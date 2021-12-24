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

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.graph.GraphConstants.INNER_CAPACITY;
import static com.google.common.graph.GraphConstants.INNER_LOAD_FACTOR;
import static java.util.Objects.requireNonNull;

/**
 * An implementation of {@link NetworkConnections} for undirected networks with parallel edges.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
final class UndirectedMultiNetworkConnections<N, E>
        extends AbstractUndirectedNetworkConnections<N, E> {

    private UndirectedMultiNetworkConnections(Map<E, N> incidentEdges) {
        super(incidentEdges);
    }

    static <N, E> UndirectedMultiNetworkConnections<N, E> of() {
        return new UndirectedMultiNetworkConnections<>(
                new HashMap<E, N>(INNER_CAPACITY, INNER_LOAD_FACTOR));
    }

    static <N, E> UndirectedMultiNetworkConnections<N, E> ofImmutable(Map<E, N> incidentEdges) {
        return new UndirectedMultiNetworkConnections<>(Collections.unmodifiableMap(new LinkedHashMap<>(incidentEdges)));
    }

    private transient Reference<Map<N, Integer>> adjacentNodesReference;

    @Override
    public Set<N> adjacentNodes() {
        return Collections.unmodifiableSet(adjacentNodesMultiset().keySet());
    }

    private Map<N, Integer> adjacentNodesMultiset() {
        Map<N, Integer> adjacentNodes = getReference(adjacentNodesReference);
        if (adjacentNodes == null) {
            adjacentNodes = new HashMap<>();
            for (N node : incidentEdgeMap.values()) {
                Integer count = adjacentNodes.getOrDefault(node, 0);
                adjacentNodes.put(node, count + 1);
            }
            adjacentNodesReference = new SoftReference<>(adjacentNodes);
        }
        return adjacentNodes;
    }

    @Override
    public Set<E> edgesConnecting(N node) {
        return new MultiEdgesConnecting<E>(incidentEdgeMap, node) {
            @Override
            public int size() {
                return adjacentNodesMultiset().getOrDefault(node, 0);
            }
        };
    }

    @Override
    public N removeInEdge(E edge, boolean isSelfLoop) {
        if (!isSelfLoop) {
            return removeOutEdge(edge);
        }
        return null;
    }

    @Override
    public N removeOutEdge(E edge) {
        N node = super.removeOutEdge(edge);
        Map<N, Integer> adjacentNodes = getReference(adjacentNodesReference);
        if (adjacentNodes != null) {
            adjacentNodes.compute(node, (n, count) -> requireNonNull(count) == 1 ? null : count - 1);
        }
        return node;
    }

    @Override
    public void addInEdge(E edge, N node, boolean isSelfLoop) {
        if (!isSelfLoop) {
            addOutEdge(edge, node);
        }
    }

    @Override
    public void addOutEdge(E edge, N node) {
        super.addOutEdge(edge, node);
        Map<N, Integer> adjacentNodes = getReference(adjacentNodesReference);
        if (adjacentNodes != null) {
            adjacentNodes.compute(node, (n, count) -> count == null ? 1 : count + 1);
        }
    }

    private static <T> T getReference(Reference<T> reference) {
        return (reference == null) ? null : reference.get();
    }
}
