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
 * An implementation of {@link NetworkConnections} for directed networks with parallel edges.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
final class DirectedMultiNetworkConnections<N, E> extends AbstractDirectedNetworkConnections<N, E> {

    private DirectedMultiNetworkConnections(
            Map<E, N> inEdges, Map<E, N> outEdges, int selfLoopCount) {
        super(inEdges, outEdges, selfLoopCount);
    }

    static <N, E> DirectedMultiNetworkConnections<N, E> of() {
        return new DirectedMultiNetworkConnections<>(
                new HashMap<>(INNER_CAPACITY, INNER_LOAD_FACTOR),
                new HashMap<>(INNER_CAPACITY, INNER_LOAD_FACTOR),
                0);
    }

    static <N, E> DirectedMultiNetworkConnections<N, E> ofImmutable(
            Map<E, N> inEdges, Map<E, N> outEdges, int selfLoopCount) {
        return new DirectedMultiNetworkConnections<>(
                Collections.unmodifiableMap(new LinkedHashMap<>(inEdges)),
                Collections.unmodifiableMap(new LinkedHashMap<>(outEdges)), selfLoopCount);
    }

    private transient Reference<Map<N, Integer>> predecessorsReference;

    @Override
    public Set<N> predecessors() {
        return Collections.unmodifiableSet(predecessorsMultiset().keySet());
    }

    private Map<N, Integer> predecessorsMultiset() {
        Map<N, Integer> predecessors = getReference(predecessorsReference);
        if (predecessors == null) {
            predecessors = new HashMap<>();
            for (N node : inEdgeMap.values()) {
                Integer count = predecessors.getOrDefault(node, 0);
                predecessors.put(node, count + 1);
            }
            predecessorsReference = new SoftReference<>(predecessors);
        }
        return predecessors;
    }

    private transient Reference<Map<N, Integer>> successorsReference;

    @Override
    public Set<N> successors() {
        return Collections.unmodifiableSet(successorsMultiset().keySet());
    }

    private Map<N, Integer> successorsMultiset() {
        Map<N, Integer> successors = getReference(successorsReference);
        if (successors == null) {
            successors = new HashMap<>();
            for (N node : outEdgeMap.values()) {
                Integer count = successors.getOrDefault(node, 0);
                successors.put(node, count + 1);
            }
            successorsReference = new SoftReference<>(successors);
        }
        return successors;
    }

    @Override
    public Set<E> edgesConnecting(N node) {
        return new MultiEdgesConnecting<>(outEdgeMap, node) {
            @Override
            public int size() {
                return successorsMultiset().getOrDefault(node, 0);
            }
        };
    }

    @Override
    public N removeInEdge(E edge, boolean isSelfLoop) {
        N node = super.removeInEdge(edge, isSelfLoop);
        Map<N, Integer> predecessors = getReference(predecessorsReference);
        if (predecessors != null) {
            predecessors.compute(node, (n, count) -> requireNonNull(count) == 1 ? null : count - 1);
        }
        return node;
    }

    @Override
    public N removeOutEdge(E edge) {
        N node = super.removeOutEdge(edge);
        Map<N, Integer> successors = getReference(successorsReference);
        if (successors != null) {
            successors.compute(node, (n, count) -> requireNonNull(count) == 1 ? null : count - 1);
        }
        return node;
    }

    @Override
    public void addInEdge(E edge, N node, boolean isSelfLoop) {
        super.addInEdge(edge, node, isSelfLoop);
        Map<N, Integer> predecessors = getReference(predecessorsReference);
        if (predecessors != null) {
            predecessors.compute(node, (n, count) -> count == null ? 1 : count + 1);
        }
    }

    @Override
    public void addOutEdge(E edge, N node) {
        super.addOutEdge(edge, node);
        Map<N, Integer> successors = getReference(successorsReference);
        if (successors != null) {
            successors.compute(node, (n, count) -> count == null ? 1 : count + 1);
        }
    }

    private static <T> T getReference(Reference<T> reference) {
        return (reference == null) ? null : reference.get();
    }
}
