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

import java.util.List;

import static com.google.common.graph.GraphConstants.PARALLEL_EDGES_NOT_ALLOWED;
import static com.google.common.graph.GraphConstants.REUSING_EDGE;
import static com.google.common.graph.GraphConstants.SELF_LOOPS_NOT_ALLOWED;
import static java.util.Objects.requireNonNull;

/**
 * Standard implementation of {@link MutableNetwork} that supports both directed and undirected
 * graphs. Instances of this class should be constructed with {@link NetworkBuilder}.
 *
 * <p>Time complexities for mutation methods are all O(1) except for {@code removeNode(N node)},
 * which is in O(d_node) where d_node is the degree of {@code node}.
 *
 * @author James Sexton
 * @author Joshua O'Madadhain
 * @author Omar Darwish
 * @param <N> Node parameter type
 * @param <E> Edge parameter type
 */
final class StandardMutableNetwork<N, E> extends StandardNetwork<N, E>
        implements MutableNetwork<N, E> {

    /** Constructs a mutable graph with the properties specified in {@code builder}. */
    StandardMutableNetwork(NetworkBuilder<? super N, ? super E> builder) {
        super(builder);
    }

    @Override
    public boolean addNode(N node) {
        Preconditions.checkNotNull(node, "node");

        if (containsNode(node)) {
            return false;
        }

        addNodeInternal(node);
        return true;
    }

    /**
     * Adds {@code node} to the graph and returns the associated {@link NetworkConnections}.
     *
     * @throws IllegalStateException if {@code node} is already present
     */
    private NetworkConnections<N, E> addNodeInternal(N node) {
        NetworkConnections<N, E> connections = newConnections();
        Preconditions.checkState(nodeConnections.put(node, connections) == null);
        return connections;
    }

    @Override
    public boolean addEdge(N nodeU, N nodeV, E edge) {
        Preconditions.checkNotNull(nodeU, "nodeU");
        Preconditions.checkNotNull(nodeV, "nodeV");
        Preconditions.checkNotNull(edge, "edge");

        if (containsEdge(edge)) {
            EndpointPair<N> existingIncidentNodes = incidentNodes(edge);
            EndpointPair<N> newIncidentNodes = EndpointPair.of(this, nodeU, nodeV);
            Preconditions.checkArgument(
                    existingIncidentNodes.equals(newIncidentNodes),
                    REUSING_EDGE,
                    edge,
                    existingIncidentNodes,
                    newIncidentNodes);
            return false;
        }
        NetworkConnections<N, E> connectionsU = nodeConnections.get(nodeU);
        if (!allowsParallelEdges()) {
            Preconditions.checkArgument(
                    !(connectionsU != null && connectionsU.successors().contains(nodeV)),
                    PARALLEL_EDGES_NOT_ALLOWED,
                    nodeU,
                    nodeV);
        }
        boolean isSelfLoop = nodeU.equals(nodeV);
        if (!allowsSelfLoops()) {
            Preconditions.checkArgument(!isSelfLoop, SELF_LOOPS_NOT_ALLOWED, nodeU);
        }

        if (connectionsU == null) {
            connectionsU = addNodeInternal(nodeU);
        }
        connectionsU.addOutEdge(edge, nodeV);
        NetworkConnections<N, E> connectionsV = nodeConnections.get(nodeV);
        if (connectionsV == null) {
            connectionsV = addNodeInternal(nodeV);
        }
        connectionsV.addInEdge(edge, nodeU, isSelfLoop);
        edgeToReferenceNode.put(edge, nodeU);
        return true;
    }

    @Override
    public boolean addEdge(EndpointPair<N> endpoints, E edge) {
        validateEndpoints(endpoints);
        return addEdge(endpoints.nodeU(), endpoints.nodeV(), edge);
    }

    @Override
    public boolean removeNode(N node) {
        Preconditions.checkNotNull(node, "node");

        NetworkConnections<N, E> connections = nodeConnections.get(node);
        if (connections == null) {
            return false;
        }

        // Since views are returned, we need to copy the edges that will be removed.
        // Thus we avoid modifying the underlying view while iterating over it.
        for (E edge : List.copyOf(connections.incidentEdges())) {
            removeEdge(edge);
        }
        nodeConnections.remove(node);
        return true;
    }

    @Override
    public boolean removeEdge(E edge) {
        Preconditions.checkNotNull(edge, "edge");

        N nodeU = edgeToReferenceNode.get(edge);
        if (nodeU == null) {
            return false;
        }

        // requireNonNull is safe because of the edgeToReferenceNode check above.
        NetworkConnections<N, E> connectionsU = requireNonNull(nodeConnections.get(nodeU));
        N nodeV = connectionsU.adjacentNode(edge);
        NetworkConnections<N, E> connectionsV = requireNonNull(nodeConnections.get(nodeV));
        connectionsU.removeOutEdge(edge);
        connectionsV.removeInEdge(edge, allowsSelfLoops() && nodeU.equals(nodeV));
        edgeToReferenceNode.remove(edge);
        return true;
    }

    private NetworkConnections<N, E> newConnections() {
        return isDirected()
                ? allowsParallelEdges()
                ? DirectedMultiNetworkConnections.of()
                : DirectedNetworkConnections.of()
                : allowsParallelEdges()
                ? UndirectedMultiNetworkConnections.of()
                : UndirectedNetworkConnections.of();
    }
}
