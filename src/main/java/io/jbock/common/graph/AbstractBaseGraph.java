/*
 * Copyright (C) 2017 The Guava Authors
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

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.jbock.common.graph.GraphConstants.ENDPOINTS_MISMATCH;

/**
 * This class provides a skeletal implementation of {@link BaseGraph}.
 *
 * <p>The methods implemented in this class should not be overridden unless the subclass admits a
 * more efficient implementation.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 */
abstract class AbstractBaseGraph<N> implements BaseGraph<N> {

    /**
     * Returns the number of edges in this graph; used to calculate the size of {@link #edges()}. This
     * implementation requires O(|N|) time. Classes extending this one may manually keep track of the
     * number of edges as the graph is updated, and override this method for better performance.
     */
    protected long edgeCount() {
        long degreeSum = 0L;
        for (N node : nodes()) {
            degreeSum += degree(node);
        }
        // According to the degree sum formula, this is equal to twice the number of edges.
        Preconditions.checkState((degreeSum & 1) == 0);
        return degreeSum >>> 1;
    }

    /**
     * An implementation of {@link BaseGraph#edges()} defined in terms of {@link #nodes()} and {@link
     * #successors(Object)}.
     */
    @Override
    public Set<EndpointPair<N>> edges() {
        return new AbstractSet<>() {
            @Override
            public Iterator<EndpointPair<N>> iterator() {
                return EndpointPairIterator.of(AbstractBaseGraph.this);
            }

            @Override
            public int size() {
                return Math.toIntExact(edgeCount());
            }

            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException();
            }

            // Mostly safe: We check contains(u) before calling successors(u), so we perform unsafe
            // operations only in weird cases like checking for an EndpointPair<ArrayList> in a
            // Graph<LinkedList>.
            @SuppressWarnings("unchecked")
            @Override
            public boolean contains(Object obj) {
                if (!(obj instanceof EndpointPair)) {
                    return false;
                }
                EndpointPair<?> endpointPair = (EndpointPair<?>) obj;
                return isOrderingCompatible(endpointPair)
                        && nodes().contains(endpointPair.nodeU())
                        && successors((N) endpointPair.nodeU()).contains(endpointPair.nodeV());
            }
        };
    }

    @Override
    public ElementOrder<N> incidentEdgeOrder() {
        return ElementOrder.unordered();
    }

    @Override
    public Set<EndpointPair<N>> incidentEdges(N node) {
        Preconditions.checkNotNull(node);
        Preconditions.checkArgument(nodes().contains(node), "Node %s is not an element of this graph.", node);
        return new IncidentEdgeSet<N>(this, node) {
            @Override
            public Iterator<EndpointPair<N>> iterator() {
                if (graph.isDirected()) {
                    return Stream.of(graph.predecessors(node).stream()
                                            .map((N predecessor) -> EndpointPair.ordered(predecessor, node)),
                                    Util.difference(graph.successors(node), Set.of(node)).stream()
                                            .map((N successor) -> EndpointPair.ordered(node, successor)))
                            .flatMap(Function.identity()).iterator();
                } else {
                    return graph.adjacentNodes(node).stream()
                            .map((N adjacentNode) -> EndpointPair.unordered(node, adjacentNode))
                            .iterator();
                }
            }
        };
    }

    @Override
    public int degree(N node) {
        if (isDirected()) {
            return Math.addExact(predecessors(node).size(), successors(node).size());
        } else {
            Set<N> neighbors = adjacentNodes(node);
            int selfLoopCount = (allowsSelfLoops() && neighbors.contains(node)) ? 1 : 0;
            return Math.addExact(neighbors.size(), selfLoopCount);
        }
    }

    @Override
    public int inDegree(N node) {
        return isDirected() ? predecessors(node).size() : degree(node);
    }

    @Override
    public int outDegree(N node) {
        return isDirected() ? successors(node).size() : degree(node);
    }

    @Override
    public boolean hasEdgeConnecting(N nodeU, N nodeV) {
        Preconditions.checkNotNull(nodeU);
        Preconditions.checkNotNull(nodeV);
        return nodes().contains(nodeU) && successors(nodeU).contains(nodeV);
    }

    @Override
    public boolean hasEdgeConnecting(EndpointPair<N> endpoints) {
        Preconditions.checkNotNull(endpoints);
        if (!isOrderingCompatible(endpoints)) {
            return false;
        }
        N nodeU = endpoints.nodeU();
        N nodeV = endpoints.nodeV();
        return nodes().contains(nodeU) && successors(nodeU).contains(nodeV);
    }

    /**
     * Throws {@code IllegalArgumentException} if the ordering of {@code endpoints} is not compatible
     * with the directionality of this graph.
     */
    protected final void validateEndpoints(EndpointPair<?> endpoints) {
        Preconditions.checkNotNull(endpoints);
        Preconditions.checkArgument(isOrderingCompatible(endpoints), ENDPOINTS_MISMATCH);
    }

    protected final boolean isOrderingCompatible(EndpointPair<?> endpoints) {
        return endpoints.isOrdered() || !this.isDirected();
    }
}
