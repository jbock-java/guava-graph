/*
 * Copyright (C) 2014 The Guava Authors
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

import org.junit.After;
import org.junit.Test;

import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Abstract base class for testing undirected {@link Graph} implementations defined in this package.
 */
public abstract class AbstractStandardUndirectedGraphTest extends AbstractGraphTest {

    @After
    public void validateUndirectedEdges() {
        /*
        for (Integer node : graph.nodes()) {
            new EqualsTester()
                    .addEqualityGroup(
                            graph.predecessors(node), graph.successors(node), graph.adjacentNodes(node))
                    .testEquals();
        }
        */
    }

    @Override
    @Test
    public void nodes_checkReturnedSetMutability() {
        if (!graphIsMutable()) {
            return;
        }

        Set<Integer> nodes = graph.nodes();
        try {
            nodes.add(N2);
            fail(ERROR_MODIFIABLE_SET);
        } catch (UnsupportedOperationException e) {
            addNode(N1);
            assertThat(graph.nodes()).containsExactlyElementsIn(nodes);
        }
    }

    @Override
    @Test
    public void adjacentNodes_checkReturnedSetMutability() {
        if (!graphIsMutable()) {
            return;
        }

        addNode(N1);
        Set<Integer> adjacentNodes = graph.adjacentNodes(N1);
        try {
            adjacentNodes.add(N2);
            fail(ERROR_MODIFIABLE_SET);
        } catch (UnsupportedOperationException e) {
            putEdge(N1, N2);
            assertThat(graph.adjacentNodes(N1)).containsExactlyElementsIn(adjacentNodes);
        }
    }

    @Override
    @Test
    public void predecessors_checkReturnedSetMutability() {
        if (!graphIsMutable()) {
            return;
        }

        addNode(N2);
        Set<Integer> predecessors = graph.predecessors(N2);
        try {
            predecessors.add(N1);
            fail(ERROR_MODIFIABLE_SET);
        } catch (UnsupportedOperationException e) {
            putEdge(N1, N2);
            assertThat(graph.predecessors(N2)).containsExactlyElementsIn(predecessors);
        }
    }

    @Override
    @Test
    public void successors_checkReturnedSetMutability() {
        if (!graphIsMutable()) {
            return;
        }

        addNode(N1);
        Set<Integer> successors = graph.successors(N1);
        try {
            successors.add(N2);
            fail(ERROR_MODIFIABLE_SET);
        } catch (UnsupportedOperationException e) {
            putEdge(N1, N2);
            assertThat(graph.successors(N1)).containsExactlyElementsIn(successors);
        }
    }

    @Override
    @Test
    public void incidentEdges_checkReturnedSetMutability() {
        if (!graphIsMutable()) {
            return;
        }

        addNode(N1);
        Set<EndpointPair<Integer>> incidentEdges = graph.incidentEdges(N1);
        try {
            incidentEdges.add(EndpointPair.unordered(N1, N2));
            fail(ERROR_MODIFIABLE_SET);
        } catch (UnsupportedOperationException e) {
            putEdge(N1, N2);
            assertThat(incidentEdges).containsExactlyElementsIn(graph.incidentEdges(N1));
        }
    }

    @Test
    public void predecessors_oneEdge() {
        putEdge(N1, N2);
        assertThat(graph.predecessors(N2)).containsExactly(N1);
        assertThat(graph.predecessors(N1)).containsExactly(N2);
    }

    @Test
    public void successors_oneEdge() {
        putEdge(N1, N2);
        assertThat(graph.successors(N1)).containsExactly(N2);
        assertThat(graph.successors(N2)).containsExactly(N1);
    }

    @Test
    public void incidentEdges_oneEdge() {
        putEdge(N1, N2);
        EndpointPair<Integer> expectedEndpoints = EndpointPair.unordered(N1, N2);
        assertThat(graph.incidentEdges(N1)).containsExactly(expectedEndpoints);
        assertThat(graph.incidentEdges(N2)).containsExactly(expectedEndpoints);
    }

    @Test
    public void inDegree_oneEdge() {
        putEdge(N1, N2);
        assertThat(graph.inDegree(N2)).isEqualTo(1);
        assertThat(graph.inDegree(N1)).isEqualTo(1);
    }

    @Test
    public void outDegree_oneEdge() {
        putEdge(N1, N2);
        assertThat(graph.outDegree(N1)).isEqualTo(1);
        assertThat(graph.outDegree(N2)).isEqualTo(1);
    }

    @Test
    public void hasEdgeConnecting_correct() {
        putEdge(N1, N2);
        assertThat(graph.hasEdgeConnecting(EndpointPair.unordered(N1, N2))).isTrue();
        assertThat(graph.hasEdgeConnecting(EndpointPair.unordered(N2, N1))).isTrue();
    }

    @Test
    public void hasEdgeConnecting_mismatch() {
        putEdge(N1, N2);
        assertThat(graph.hasEdgeConnecting(EndpointPair.ordered(N1, N2))).isTrue();
        assertThat(graph.hasEdgeConnecting(EndpointPair.ordered(N2, N1))).isTrue();
    }

    @Test
    public void adjacentNodes_selfLoop() {
        if (!graph.allowsSelfLoops()) {
            return;
        }

        putEdge(N1, N1);
        putEdge(N1, N2);
        assertThat(graph.adjacentNodes(N1)).containsExactly(N1, N2);
    }

    @Test
    public void predecessors_selfLoop() {
        if (!graph.allowsSelfLoops()) {
            return;
        }

        putEdge(N1, N1);
        assertThat(graph.predecessors(N1)).containsExactly(N1);
        putEdge(N1, N2);
        assertThat(graph.predecessors(N1)).containsExactly(N1, N2);
    }

    @Test
    public void successors_selfLoop() {
        if (!graph.allowsSelfLoops()) {
            return;
        }

        putEdge(N1, N1);
        assertThat(graph.successors(N1)).containsExactly(N1);
        putEdge(N2, N1);
        assertThat(graph.successors(N1)).containsExactly(N1, N2);
    }

    @Test
    public void incidentEdges_selfLoop() {
        if (!graph.allowsSelfLoops()) {
            return;
        }

        putEdge(N1, N1);
        assertThat(graph.incidentEdges(N1)).containsExactly(EndpointPair.unordered(N1, N1));
        putEdge(N1, N2);
        assertThat(graph.incidentEdges(N1))
                .containsExactly(EndpointPair.unordered(N1, N1), EndpointPair.unordered(N1, N2));
    }

    @Test
    public void degree_selfLoop() {
        if (!graph.allowsSelfLoops()) {
            return;
        }

        putEdge(N1, N1);
        assertThat(graph.degree(N1)).isEqualTo(2);
        putEdge(N1, N2);
        assertThat(graph.degree(N1)).isEqualTo(3);
    }

    @Test
    public void inDegree_selfLoop() {
        if (!graph.allowsSelfLoops()) {
            return;
        }

        putEdge(N1, N1);
        assertThat(graph.inDegree(N1)).isEqualTo(2);
        putEdge(N1, N2);
        assertThat(graph.inDegree(N1)).isEqualTo(3);
    }

    @Test
    public void outDegree_selfLoop() {
        if (!graph.allowsSelfLoops()) {
            return;
        }

        putEdge(N1, N1);
        assertThat(graph.outDegree(N1)).isEqualTo(2);
        putEdge(N2, N1);
        assertThat(graph.outDegree(N1)).isEqualTo(3);
    }

    // Stable order tests

    // Note: Stable order means that the ordering doesn't change between iterations and versions.
    // Ideally, the ordering in test should never be updated.
    @Test
    public void stableIncidentEdgeOrder_edges_returnsInStableOrder() {
        if (graph.incidentEdgeOrder().type() != ElementOrder.Type.STABLE) {
            return;
        }

        populateTShapedGraph();

        assertThat(graph.edges())
                .containsExactly(
                        EndpointPair.unordered(1, 2),
                        EndpointPair.unordered(1, 4),
                        EndpointPair.unordered(1, 3),
                        EndpointPair.unordered(4, 5))
                .inOrder();
    }

    @Test
    public void stableIncidentEdgeOrder_adjacentNodes_returnsInConnectingEdgeInsertionOrder() {
        if (graph.incidentEdgeOrder().type() != ElementOrder.Type.STABLE) {
            return;
        }

        populateTShapedGraph();

        assertThat(graph.adjacentNodes(1)).containsExactly(2, 4, 3).inOrder();
    }

    @Test
    public void stableIncidentEdgeOrder_predecessors_returnsInConnectingEdgeInsertionOrder() {
        if (graph.incidentEdgeOrder().type() != ElementOrder.Type.STABLE) {
            return;
        }

        populateTShapedGraph();

        assertThat(graph.adjacentNodes(1)).containsExactly(2, 4, 3).inOrder();
    }

    @Test
    public void stableIncidentEdgeOrder_successors_returnsInConnectingEdgeInsertionOrder() {
        if (graph.incidentEdgeOrder().type() != ElementOrder.Type.STABLE) {
            return;
        }

        populateTShapedGraph();

        assertThat(graph.adjacentNodes(1)).containsExactly(2, 4, 3).inOrder();
    }

    @Test
    public void stableIncidentEdgeOrder_incidentEdges_returnsInEdgeInsertionOrder() {
        if (graph.incidentEdgeOrder().type() != ElementOrder.Type.STABLE) {
            return;
        }

        populateTShapedGraph();

        assertThat(graph.incidentEdges(1))
                .containsExactly(
                        EndpointPair.unordered(1, 2),
                        EndpointPair.unordered(1, 4),
                        EndpointPair.unordered(1, 3))
                .inOrder();
    }

    @Test
    public void stableIncidentEdgeOrder_incidentEdges_withSelfLoop_returnsInEdgeInsertionOrder() {
        if (graph.incidentEdgeOrder().type() != ElementOrder.Type.STABLE) {
            return;
        }
        if (!graph.allowsSelfLoops()) {
            return;
        }

        putEdge(2, 1);
        putEdge(1, 1);
        putEdge(1, 3);

        assertThat(graph.incidentEdges(1))
                .containsExactly(
                        EndpointPair.unordered(2, 1),
                        EndpointPair.unordered(1, 1),
                        EndpointPair.unordered(1, 3))
                .inOrder();
    }

    /**
     * Populates the graph with nodes and edges in a star shape with node `1` in the middle.
     *
     * <p>Note that the edges are added in a shuffled order to properly test the effect of the
     * insertion order.
     */
    private void populateTShapedGraph() {
        putEdge(2, 1);
        putEdge(1, 4);
        putEdge(1, 3);
        putEdge(1, 2); // Duplicate
        putEdge(4, 5);
    }

    // Element Mutation

    @Test
    public void putEdge_existingNodes() {
        if (!graphIsMutable()) {
            return;
        }

        // Adding nodes initially for safety (insulating from possible future
        // modifications to proxy methods)
        addNode(N1);
        addNode(N2);

        assertThat(graphAsMutableGraph.putEdge(N1, N2)).isTrue();
    }

    @Test
    public void putEdge_existingEdgeBetweenSameNodes() {
        if (!graphIsMutable()) {
            return;
        }

        putEdge(N1, N2);

        assertThat(graphAsMutableGraph.putEdge(N2, N1)).isFalse();
    }

    /**
     * Tests that the method {@code putEdge} will silently add the missing nodes to the graph, then
     * add the edge connecting them. We are not using the proxy methods here as we want to test {@code
     * putEdge} when the end-points are not elements of the graph.
     */
    @Test
    public void putEdge_nodesNotInGraph() {
        if (!graphIsMutable()) {
            return;
        }

        graphAsMutableGraph.addNode(N1);
        assertTrue(graphAsMutableGraph.putEdge(N1, N5));
        assertTrue(graphAsMutableGraph.putEdge(N4, N1));
        assertTrue(graphAsMutableGraph.putEdge(N2, N3));
        assertThat(graph.nodes()).containsExactly(N1, N5, N4, N2, N3).inOrder();
        assertThat(graph.adjacentNodes(N1)).containsExactly(N4, N5);
        assertThat(graph.adjacentNodes(N2)).containsExactly(N3);
        assertThat(graph.adjacentNodes(N3)).containsExactly(N2);
        assertThat(graph.adjacentNodes(N4)).containsExactly(N1);
        assertThat(graph.adjacentNodes(N5)).containsExactly(N1);
    }

    @Test
    public void putEdge_doesntAllowSelfLoops() {
        if (!graphIsMutable()) {
            return;
        }
        if (graph.allowsSelfLoops()) {
            return;
        }

        try {
            putEdge(N1, N1);
            fail(ERROR_ADDED_SELF_LOOP);
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessageThat().contains(ERROR_SELF_LOOP);
        }
    }

    @Test
    public void putEdge_allowsSelfLoops() {
        if (!graphIsMutable()) {
            return;
        }
        if (!graph.allowsSelfLoops()) {
            return;
        }

        assertThat(graphAsMutableGraph.putEdge(N1, N1)).isTrue();
        assertThat(graph.adjacentNodes(N1)).containsExactly(N1);
    }

    @Test
    public void putEdge_existingSelfLoopEdgeBetweenSameNodes() {
        if (!graphIsMutable()) {
            return;
        }
        if (!graph.allowsSelfLoops()) {
            return;
        }

        putEdge(N1, N1);
        assertThat(graphAsMutableGraph.putEdge(N1, N1)).isFalse();
    }

    @Test
    public void removeEdge_antiparallelEdges() {
        if (!graphIsMutable()) {
            return;
        }

        putEdge(N1, N2);
        putEdge(N2, N1); // no-op

        assertThat(graphAsMutableGraph.removeEdge(N1, N2)).isTrue();
        assertThat(graph.adjacentNodes(N1)).isEmpty();
        assertThat(graph.edges()).isEmpty();
        assertThat(graphAsMutableGraph.removeEdge(N2, N1)).isFalse();
    }

    @Test
    public void removeNode_existingNodeWithSelfLoopEdge() {
        if (!graphIsMutable()) {
            return;
        }
        if (!graph.allowsSelfLoops()) {
            return;
        }

        addNode(N1);
        putEdge(N1, N1);
        assertThat(graphAsMutableGraph.removeNode(N1)).isTrue();
        assertThat(graph.nodes()).isEmpty();
    }

    @Test
    public void removeEdge_existingSelfLoopEdge() {
        if (!graphIsMutable()) {
            return;
        }
        if (!graph.allowsSelfLoops()) {
            return;
        }

        putEdge(N1, N1);
        assertThat(graphAsMutableGraph.removeEdge(N1, N1)).isTrue();
        assertThat(graph.nodes()).containsExactly(N1);
        assertThat(graph.adjacentNodes(N1)).isEmpty();
    }
}
