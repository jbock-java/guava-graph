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
import java.util.Map;
import java.util.Set;

import static io.jbock.common.graph.Graphs.checkNonNegative;
import static io.jbock.common.graph.Graphs.checkPositive;
import static java.util.Objects.requireNonNull;

/**
 * A base implementation of {@link NetworkConnections} for directed networks.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
abstract class AbstractDirectedNetworkConnections<N, E> implements NetworkConnections<N, E> {
    /** Keys are edges incoming to the origin node, values are the source node. */
    final Map<E, N> inEdgeMap;

    /** Keys are edges outgoing from the origin node, values are the target node. */
    final Map<E, N> outEdgeMap;

    private int selfLoopCount;

    AbstractDirectedNetworkConnections(Map<E, N> inEdgeMap, Map<E, N> outEdgeMap, int selfLoopCount) {
        this.inEdgeMap = Preconditions.checkNotNull(inEdgeMap);
        this.outEdgeMap = Preconditions.checkNotNull(outEdgeMap);
        this.selfLoopCount = checkNonNegative(selfLoopCount);
        Preconditions.checkState(selfLoopCount <= inEdgeMap.size() && selfLoopCount <= outEdgeMap.size());
    }

    @Override
    public Set<N> adjacentNodes() {
        return Util.union(predecessors(), successors());
    }

    @Override
    public Set<E> incidentEdges() {
        Set<E> inEdges = inEdgeMap.keySet();
        Set<E> outEdges = outEdgeMap.keySet();
        return Util.union(inEdges, outEdges);
    }

    @Override
    public Set<E> inEdges() {
        return Collections.unmodifiableSet(inEdgeMap.keySet());
    }

    @Override
    public Set<E> outEdges() {
        return Collections.unmodifiableSet(outEdgeMap.keySet());
    }

    @Override
    public N adjacentNode(E edge) {
        // Since the reference node is defined to be 'source' for directed graphs,
        // we can assume this edge lives in the set of outgoing edges.
        // (We're relying on callers to call this method only with an edge that's in the graph.)
        return requireNonNull(outEdgeMap.get(edge));
    }

    @Override
    public N removeInEdge(E edge, boolean isSelfLoop) {
        if (isSelfLoop) {
            checkNonNegative(--selfLoopCount);
        }
        N previousNode = inEdgeMap.remove(edge);
        // We're relying on callers to call this method only with an edge that's in the graph.
        return requireNonNull(previousNode);
    }

    @Override
    public N removeOutEdge(E edge) {
        N previousNode = outEdgeMap.remove(edge);
        // We're relying on callers to call this method only with an edge that's in the graph.
        return requireNonNull(previousNode);
    }

    @Override
    public void addInEdge(E edge, N node, boolean isSelfLoop) {
        Preconditions.checkNotNull(edge);
        Preconditions.checkNotNull(node);

        if (isSelfLoop) {
            checkPositive(++selfLoopCount);
        }
        N previousNode = inEdgeMap.put(edge, node);
        Preconditions.checkState(previousNode == null);
    }

    @Override
    public void addOutEdge(E edge, N node) {
        Preconditions.checkNotNull(edge);
        Preconditions.checkNotNull(node);

        N previousNode = outEdgeMap.put(edge, node);
        Preconditions.checkState(previousNode == null);
    }
}
