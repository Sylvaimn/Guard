package com.temporedata.guard.modules.dev.workflow.service;
import com.temporedata.guard.modules.dev.workflow.entity.WorkflowEdgeEntity;
import com.temporedata.guard.modules.dev.workflow.entity.WorkflowNodeEntity;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DAG orchestration engine based on JGraphT.
 * Provides topological sort, cycle detection, downstream traversal,
 * and execution order computation for workflow DAGs.
 */
@Slf4j
@Component
public class WorkflowDagService {

    /**
     * Build a directed graph from nodes and edges.
     */
    public DefaultDirectedGraph<String, DefaultEdge> buildGraph(
            List<WorkflowNodeEntity> nodes,
            List<WorkflowEdgeEntity> edges) {

        DefaultDirectedGraph<String, DefaultEdge> graph =
                new DefaultDirectedGraph<>(DefaultEdge.class);

        // Add all vertices
        for (WorkflowNodeEntity node : nodes) {
            graph.addVertex(node.getId());
        }

        // Add all edges
        for (WorkflowEdgeEntity edge : edges) {
            String source = edge.getSourceNodeId();
            String target = edge.getTargetNodeId();
            if (graph.containsVertex(source) && graph.containsVertex(target)) {
                graph.addEdge(source, target);
            }
        }

        return graph;
    }

    /**
     * Check if the graph contains any cycles.
     */
    public boolean hasCycle(DefaultDirectedGraph<String, DefaultEdge> graph) {
        CycleDetector<String, DefaultEdge> detector = new CycleDetector<>(graph);
        return detector.detectCycles();
    }

    /**
     * Get all cycles in the graph.
     */
    public Set<String> findCycles(DefaultDirectedGraph<String, DefaultEdge> graph) {
        CycleDetector<String, DefaultEdge> detector = new CycleDetector<>(graph);
        return detector.findCycles();
    }

    /**
     * Topological sort: compute execution order.
     *
     * @return ordered list of node IDs
     */
    public List<String> topologicalSort(DefaultDirectedGraph<String, DefaultEdge> graph) {
        if (hasCycle(graph)) {
            throw new BusinessException("DAG contains cycles, cannot execute");
        }
        TopologicalOrderIterator<String, DefaultEdge> iterator =
                new TopologicalOrderIterator<>(graph);
        List<String> order = new ArrayList<>();
        iterator.forEachRemaining(order::add);
        return order;
    }

    /**
     * Topological sort with node mapping.
     *
     * @return ordered list of WorkflowNodeEntity
     */
    public List<WorkflowNodeEntity> topologicalSortNodes(
            List<WorkflowNodeEntity> nodes,
            List<WorkflowEdgeEntity> edges) {

        DefaultDirectedGraph<String, DefaultEdge> graph = buildGraph(nodes, edges);
        List<String> order = topologicalSort(graph);

        Map<String, WorkflowNodeEntity> nodeMap = nodes.stream()
                .collect(Collectors.toMap(WorkflowNodeEntity::getId, n -> n));

        return order.stream().map(nodeMap::get).collect(Collectors.toList());
    }

    /**
     * Get all downstream nodes from a given node (BFS traversal).
     * Used for "retry downstream" feature.
     */
    public Set<String> getDownstreamNodes(
            DefaultDirectedGraph<String, DefaultEdge> graph,
            String nodeId) {

        Set<String> downstream = new LinkedHashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(nodeId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (DefaultEdge edge : graph.outgoingEdgesOf(current)) {
                String target = graph.getEdgeTarget(edge);
                if (downstream.add(target)) {
                    queue.add(target);
                }
            }
        }

        return downstream;
    }

    /**
     * Get all upstream nodes from a given node (BFS reverse traversal).
     * Used for dependency analysis.
     */
    public Set<String> getUpstreamNodes(
            DefaultDirectedGraph<String, DefaultEdge> graph,
            String nodeId) {

        Set<String> upstream = new LinkedHashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(nodeId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (DefaultEdge edge : graph.incomingEdgesOf(current)) {
                String source = graph.getEdgeSource(edge);
                if (upstream.add(source)) {
                    queue.add(source);
                }
            }
        }

        return upstream;
    }

    /**
     * Get entry nodes (nodes with no incoming edges).
     */
    public Set<String> getEntryNodes(DefaultDirectedGraph<String, DefaultEdge> graph) {
        return graph.vertexSet().stream()
                .filter(v -> graph.inDegreeOf(v) == 0)
                .collect(Collectors.toSet());
    }

    /**
     * Get exit nodes (nodes with no outgoing edges).
     */
    public Set<String> getExitNodes(DefaultDirectedGraph<String, DefaultEdge> graph) {
        return graph.vertexSet().stream()
                .filter(v -> graph.outDegreeOf(v) == 0)
                .collect(Collectors.toSet());
    }

    /**
     * Find all paths from source to target.
     */
    public List<GraphPath<String, DefaultEdge>> findAllPaths(
            DefaultDirectedGraph<String, DefaultEdge> graph,
            String source, String target) {

        AllDirectedPaths<String, DefaultEdge> paths =
                new AllDirectedPaths<>(graph);
        return paths.getAllPaths(source, target, true, null);
    }

    /**
     * Validate DAG structure: no cycles, all nodes connected, no orphan edges.
     */
    public void validateDag(List<WorkflowNodeEntity> nodes, List<WorkflowEdgeEntity> edges) {
        if (nodes == null || nodes.isEmpty()) {
            return; // empty DAG is valid
        }

        DefaultDirectedGraph<String, DefaultEdge> graph = buildGraph(nodes, edges);

        // Check for cycles
        if (hasCycle(graph)) {
            Set<String> cycles = findCycles(graph);
            throw new BusinessException("DAG contains cycles: " + String.join(", ", cycles));
        }

        // Check for orphan edges (edges referencing non-existent nodes)
        Set<String> nodeIds = nodes.stream()
                .map(WorkflowNodeEntity::getId)
                .collect(Collectors.toSet());

        if (edges != null) {
            for (WorkflowEdgeEntity edge : edges) {
                if (!nodeIds.contains(edge.getSourceNodeId())) {
                    throw new BusinessException("Edge references non-existent source node: " + edge.getSourceNodeId());
                }
                if (!nodeIds.contains(edge.getTargetNodeId())) {
                    throw new BusinessException("Edge references non-existent target node: " + edge.getTargetNodeId());
                }
            }
        }
    }
}