package Models;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

public class CityMap {
    private Graph<String, DefaultWeightedEdge> cityGraph;
    private static CityMap instance;

    private CityMap(int numNodes, int numEdges) {
        cityGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        Random random = new Random();
        List<String> nodes = new ArrayList<>();
        List<String[]> edges = new ArrayList<>();

        // Generar nombres de nodos (A, B, C, ...)
        for (int i = 0; i < numNodes; i++) {
            // String nodeName =  UUID.randomUUID().toString();
            String nodeName = String.valueOf((char) ('A' + i));
            nodes.add(nodeName);
            addLocation(nodeName);
        }

        // Crear un grafo conectado generando un árbol básico
        for (int i = 1; i < numNodes; i++) {
            String node1 = nodes.get(i - 1);
            String node2 = nodes.get(i);
            int weight = random.nextInt(100) + 1; // Peso aleatorio entre 1 y 100
            addRoad(node1, node2, weight);
            edges.add(new String[]{node1, node2, String.valueOf(weight)});
        }

        // Agregar aristas adicionales al grafo
        int additionalEdges = numEdges - (numNodes - 1);
        while (additionalEdges > 0) {
            String node1 = nodes.get(random.nextInt(numNodes));
            String node2 = nodes.get(random.nextInt(numNodes));

            // Evitar bucles y duplicados
            if (!node1.equals(node2) && edges.stream().noneMatch(edge ->
                    (edge[0].equals(node1) && edge[1].equals(node2)) || 
                    (edge[0].equals(node2) && edge[1].equals(node1)))) {
                int weight = random.nextInt(100) + 1; // Peso aleatorio entre 1 y 100
                addRoad(node1, node2, weight);
                edges.add(new String[]{node1, node2, String.valueOf(weight)});
                additionalEdges--;
            }
        }

        // Opcional: Verificar el resultado
        // System.out.println("Generated nodes: " + nodes);
        // System.out.println("Generated edges: " + edges);

        //Generar un archivo DOT para visualizar el grafo
        generateDotFile("graph.dot", nodes, edges);
    }

    public static synchronized CityMap getInstance(int numNodes, int numEdges) {
        if (instance == null) {
            instance = new CityMap(numNodes, numEdges);
        }
        return instance;
    }

    public void addLocation(String location) {
        cityGraph.addVertex(location);
    }

    public void addRoad(String loc1, String loc2, double distance) {
        DefaultWeightedEdge edge = cityGraph.addEdge(loc1, loc2);
        if (edge != null) { // Evitar duplicados
            cityGraph.setEdgeWeight(edge, distance);
            // System.out.println(edge + " " + distance);
        }
    }

    public Graph<String, DefaultWeightedEdge> getGraph() {
        return cityGraph;
    }

    private void generateDotFile(String fileName, List<String> nodes, List<String[]> edges) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write("graph G {\n");

            // Agregar nodos
            for (String node : nodes) {
                writer.write("  " + node + ";\n");
            }

            // Agregar aristas con pesos
            for (String[] edge : edges) {
                writer.write("  " + edge[0] + " -- " + edge[1] + " [label=" + edge[2] + "];\n");
            }

            writer.write("}");
            // System.out.println("Graph written to " + fileName);
        } catch (IOException e) {
            System.err.println("Error writing graph to file: " + e.getMessage());
        }
    }
}
