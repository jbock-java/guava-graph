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

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.jbock.common.graph.GraphConstants.ENDPOINTS_MISMATCH;
import static io.jbock.common.graph.GraphConstants.MULTIPLE_EDGES_CONNECTING;

/**
 * This class provides a skeletal implementation of {@link Network}. It is recommended to extend
 * this class rather than implement {@link Network} directly.
 *
 * <p>The methods implemented in this class should not be overridden unless the subclass admits a
 * more efficient implementation.
 *
 * @author James Sexton
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 * @since 20.0
 */
public abstract class AbstractNetwork<N, E> implements Network<N, E> {

    @Override
    public Graph<N> asGraph() {
        return new AbstractGraph<N>() {
            @Override
            public Set<N> nodes() {
                return AbstractNetwork.this.nodes();
            }

            @Override
            public Set<EndpointPair<N>> edges() {
                if (allowsParallelEdges()) {
                    return super.edges(); // Defer to AbstractGraph implementation.
                }

                // Optimized implementation assumes no parallel edges (1:1 edge to EndpointPair mapping).
                return new AbstractSet<>() {
                    @Override
                    public Iterator<EndpointPair<N>> iterator() {
                        return AbstractNetwork.this.edges().stream()
                                .map(AbstractNetwork.this::incidentNodes)
                                .iterator();
                    }

                    @Override
                    public int size() {
                        return AbstractNetwork.this.edges().size();
                    }

                    // Mostly safe: We check contains(u) before calling successors(u), so we perform unsafe
                    // operations only in weird cases like checking for an EndpointPair<ArrayList> in a
                    // Network<LinkedList>.
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
            public ElementOrder<N> nodeOrder() {
                return AbstractNetwork.this.nodeOrder();
            }

            @Override
            public ElementOrder<N> incidentEdgeOrder() {
                // TODO(b/142723300): Return AbstractNetwork.this.incidentEdgeOrder() once Network has that
                //   method.
                return ElementOrder.unordered();
            }

            @Override
            public boolean isDirected() {
                return AbstractNetwork.this.isDirected();
            }

            @Override
            public boolean allowsSelfLoops() {
                return AbstractNetwork.this.allowsSelfLoops();
            }

            @Override
            public Set<N> adjacentNodes(N node) {
                return AbstractNetwork.this.adjacentNodes(node);
            }

            @Override
            public Set<N> predecessors(N node) {
                return AbstractNetwork.this.predecessors(node);
            }

            @Override
            public Set<N> successors(N node) {
                return AbstractNetwork.this.successors(node);
            }

            // DO NOT override the AbstractGraph *degree() implementations.
        };
    }

    @Override
    public int degree(N node) {
        if (isDirected()) {
            return Math.addExact(inEdges(node).size(), outEdges(node).size());
        } else {
            return Math.addExact(incidentEdges(node).size(), edgesConnecting(node, node).size());
        }
    }

    @Override
    public int inDegree(N node) {
        return isDirected() ? inEdges(node).size() : degree(node);
    }

    @Override
    public int outDegree(N node) {
        return isDirected() ? outEdges(node).size() : degree(node);
    }

    @Override
    public Set<E> adjacentEdges(E edge) {
        EndpointPair<N> endpointPair = incidentNodes(edge); // Verifies that edge is in this network.
        Set<E> endpointPairIncidentEdges =
                Util.union(incidentEdges(endpointPair.nodeU()), incidentEdges(endpointPair.nodeV()));
        return Util.difference(endpointPairIncidentEdges, Set.of(edge));
    }

    @Override
    public Set<E> edgesConnecting(N nodeU, N nodeV) {
        Set<E> outEdgesU = outEdges(nodeU);
        Set<E> inEdgesV = inEdges(nodeV);
        return outEdgesU.size() <= inEdgesV.size()
                ? outEdgesU.stream().filter(connectedPredicate(nodeU, nodeV)).collect(Collectors.toCollection(LinkedHashSet::new))
                : inEdgesV.stream().filter(connectedPredicate(nodeU, nodeV)).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Set<E> edgesConnecting(EndpointPair<N> endpoints) {
        validateEndpoints(endpoints);
        return edgesConnecting(endpoints.nodeU(), endpoints.nodeV());
    }

    private Predicate<E> connectedPredicate(final N nodePresent, final N nodeToCheck) {
        return edge -> incidentNodes(edge).adjacentNode(nodePresent).equals(nodeToCheck);
    }

    @Override
    public Optional<E> edgeConnecting(N nodeU, N nodeV) {
        return Optional.ofNullable(edgeConnectingOrNull(nodeU, nodeV));
    }

    @Override
    public Optional<E> edgeConnecting(EndpointPair<N> endpoints) {
        validateEndpoints(endpoints);
        return edgeConnecting(endpoints.nodeU(), endpoints.nodeV());
    }

    @Override
    public E edgeConnectingOrNull(N nodeU, N nodeV) {
        Set<E> edgesConnecting = edgesConnecting(nodeU, nodeV);
        switch (edgesConnecting.size()) {
            case 0:
                return null;
            case 1:
                return edgesConnecting.iterator().next();
            default:
                throw new IllegalArgumentException(String.format(MULTIPLE_EDGES_CONNECTING, nodeU, nodeV));
        }
    }

    @Override
    public E edgeConnectingOrNull(EndpointPair<N> endpoints) {
        validateEndpoints(endpoints);
        return edgeConnectingOrNull(endpoints.nodeU(), endpoints.nodeV());
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
        return hasEdgeConnecting(endpoints.nodeU(), endpoints.nodeV());
    }

    /**
     * Throws an IllegalArgumentException if the ordering of {@code endpoints} is not compatible with
     * the directionality of this graph.
     */
    protected final void validateEndpoints(EndpointPair<?> endpoints) {
        Preconditions.checkNotNull(endpoints);
        Preconditions.checkArgument(isOrderingCompatible(endpoints), ENDPOINTS_MISMATCH);
    }

    protected final boolean isOrderingCompatible(EndpointPair<?> endpoints) {
        return endpoints.isOrdered() || !this.isDirected();
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Network)) {
            return false;
        }
        Network<?, ?> other = (Network<?, ?>) obj;

        return isDirected() == other.isDirected()
                && nodes().equals(other.nodes())
                && edgeIncidentNodesMap(this).equals(edgeIncidentNodesMap(other));
    }

    @Override
    public final int hashCode() {
        return edgeIncidentNodesMap(this).hashCode();
    }

    /** Returns a string representation of this network. */
    @Override
    public String toString() {
        return "isDirected: "
                + isDirected()
                + ", allowsParallelEdges: "
                + allowsParallelEdges()
                + ", allowsSelfLoops: "
                + allowsSelfLoops()
                + ", nodes: "
                + nodes()
                + ", edges: "
                + edgeIncidentNodesMap(this);
    }

    private static <N, E> Map<E, EndpointPair<N>> edgeIncidentNodesMap(final Network<N, E> network) {
        Function<E, EndpointPair<N>> edgeToIncidentNodesFn = network::incidentNodes;
        return Util.asMap(network.edges(), edgeToIncidentNodesFn);
    }
}
