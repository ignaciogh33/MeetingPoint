package Agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.graph.DefaultWeightedEdge;
import Models.CityMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ParticipantAgent extends Agent {
    private String location; // Ubicación actual del agente
    private CityMap cityMap; // Mapa de la ciudad
    private Map<String, String> agentLocations = new HashMap<>(); // Mapa de ubicaciones de todos los agentes
    private int expectedAgents = 3; // Número total de agentes (incluyéndose a sí mismo)

    protected void setup() {
        // Inicializa la ubicación del agente a partir de los argumentos
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            location = (String) args[0];
        } else {
            System.out.println(getLocalName() + ": No location specified!");
            doDelete();
            return;
        }

        // Inicializa el mapa de la ciudad
        cityMap = CityMap.getInstance(4, 5);

        // Guarda su propia ubicación en el mapa
        agentLocations.put(getLocalName(), location);

        // Comportamientos
        addBehaviour(new BroadcastLocationBehaviour());
        addBehaviour(new ReceiveLocationBehaviour());

        System.out.println("Expected agents: " + expectedAgents);
        System.out.println("Current agent locations: " + agentLocations);
        System.out.println("Graph vertices: " + cityMap.getGraph().vertexSet());
        System.out.println("Graph edges: " + cityMap.getGraph().edgeSet());

    }

    // private void initializeMap(int numNodes, int numEdges) {
    //     private static CityMap instance;

    //     Random random = new Random();
    //     List<String> nodes = new ArrayList<>();
    //     List<String[]> edges = new ArrayList<>();

    //     // Generar nombres de nodos (A, B, C, ...)
    //     for (int i = 0; i < numNodes; i++) {
    //         nodes.add(String.valueOf((char) ('A' + i)));
    //         cityMap.addLocation(nodes.get(i));
    //     }

    //     // Crear un grafo conectado generando un árbol básico
    //     for (int i = 1; i < numNodes; i++) {
    //         String node1 = nodes.get(i - 1);
    //         String node2 = nodes.get(i);
    //         int weight = random.nextInt(100) + 1; // Peso aleatorio entre 1 y 100
    //         cityMap.addRoad(node1, node2, weight);
    //         edges.add(new String[]{node1, node2, String.valueOf(weight)});
    //     }

    //     // Agregar aristas adicionales al grafo
    //     int additionalEdges = numEdges - (numNodes - 1);
    //     while (additionalEdges > 0) {
    //         String node1 = nodes.get(random.nextInt(numNodes));
    //         String node2 = nodes.get(random.nextInt(numNodes));

    //         // Evitar bucles y duplicados
    //         if (!node1.equals(node2) && edges.stream().noneMatch(edge ->
    //                 (edge[0].equals(node1) && edge[1].equals(node2)) || (edge[0].equals(node2) && edge[1].equals(node1)))) {
    //             int weight = random.nextInt(100) + 1; // Peso aleatorio entre 1 y 100
    //             cityMap.addRoad(node1, node2, weight);
    //             edges.add(new String[]{node1, node2, String.valueOf(weight)});
    //             additionalEdges--;
    //         }
    //     }

    //     // Generar un archivo DOT para visualizar el grafo
    //     generateDotFile("graph.dot", nodes, edges);

    //     // Verificar el resultado (opcional)
    //     System.out.println("Generated nodes: " + nodes);
    //     System.out.println("Generated edges: " + edges);
    // }

    // private void generateDotFile(String fileName, List<String> nodes, List<String[]> edges) {
    //     try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
    //         writer.write("graph G {\n");

    //         // Agregar nodos
    //         for (String node : nodes) {
    //             writer.write("  " + node + ";\n");
    //         }

    //         // Agregar aristas con pesos
    //         for (String[] edge : edges) {
    //             writer.write("  " + edge[0] + " -- " + edge[1] + " [label=" + edge[2] + "];\n");
    //         }

    //         writer.write("}");
    //         System.out.println("Graph written to " + fileName);
    //     } catch (IOException e) {
    //         System.err.println("Error writing graph to file: " + e.getMessage());
    //     }
    // }
    // Envía la ubicación del agente a todos los demás después de un retraso de 3 segundos
    private class BroadcastLocationBehaviour extends OneShotBehaviour {
        public void action() {
            try {
                // Retraso de 3 segundos antes de enviar la ubicación
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setContent(location);

            // Envía el mensaje a todos los agentes (broadcast)
            for (int i = 1; i <= expectedAgents; i++) {
                String agentName = "participant" + i; // Suponiendo nombres participant1, participant2, etc.
                if (!agentName.equals(getLocalName())) { // No se envía a sí mismo
                    msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
                }
            }

            send(msg);
            System.out.println(getLocalName() + " sent location: " + location);
        }
    }

    // Recibe ubicaciones de otros agentes
    private class ReceiveLocationBehaviour extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                String sender = msg.getSender().getLocalName();
                String receivedLocation = msg.getContent();

                // Guarda la ubicación en el mapa si no estaba registrada
                if (!agentLocations.containsKey(sender)) {
                    agentLocations.put(sender, receivedLocation);
                    System.out.println(getLocalName() + " received location from " + sender + ": " + receivedLocation);
                    System.out.println(getLocalName() + " has locations: " + agentLocations);

                    // Calcula el Meeting Point solo si se tienen todas las ubicaciones
                    if (agentLocations.size() == expectedAgents) {
                        calculateOptimalMeetingPlace();
                    }
                }
            } else {
                block(); // Espera más mensajes
            }
        }


            private void calculateOptimalMeetingPlace() {
                Graph<String, DefaultWeightedEdge> graph = cityMap.getGraph();
                FloydWarshallShortestPaths<String, DefaultWeightedEdge> floydWarshall = new FloydWarshallShortestPaths<>(graph);
            
                // Validación de ubicaciones
                for (String agentLocation : agentLocations.values()) {
                    if (!graph.containsVertex(agentLocation)) {
                        System.out.println("Error: Location " + agentLocation + " not found in the graph.");
                        return;
                    }
                }
            
                HashMap<String, Double> totalCosts = new HashMap<>();
                HashMap<String, Double> maxCosts = new HashMap<>();
            
                // Calcular las distancias para cada nodo
                for (String location : graph.vertexSet()) {
                    double totalDistance = 0;
                    double maxDistance = 0;
            
                    for (String agentLocation : agentLocations.values()) {
                        double distance = floydWarshall.getPathWeight(agentLocation, location);
            
                        if (distance != Double.POSITIVE_INFINITY) { // Solo si hay un camino
                            totalDistance += distance;
                            maxDistance = Math.max(maxDistance, distance); // Guardar la distancia máxima
                        }
                    }
            
                    totalCosts.put(location, totalDistance);
                    maxCosts.put(location, maxDistance);
                }
            
                // Buscar el nodo óptimo (mínima suma total, luego mínima distancia máxima)
                double minTotalDistance = Double.MAX_VALUE;
                double minMaxDistance = Double.MAX_VALUE;
                String optimalLocation = null;
            
                for (String location : graph.vertexSet()) {
                    double totalDistance = totalCosts.get(location);
                    double maxDistance = maxCosts.get(location);
            
                    if (totalDistance < minTotalDistance || 
                        (totalDistance == minTotalDistance && maxDistance < minMaxDistance)) {
                        minTotalDistance = totalDistance;
                        minMaxDistance = maxDistance;
                        optimalLocation = location;
                    }
                }
            
                System.out.println(getLocalName() + " calculated optimal meeting location: " + optimalLocation 
                                   + " with total distance: " + minTotalDistance 
                                   + " and max agent distance: " + minMaxDistance);
            }
            

    }
}
