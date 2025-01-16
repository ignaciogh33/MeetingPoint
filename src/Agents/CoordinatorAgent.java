package Agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import Models.CityMap;

import java.util.HashMap;
import java.util.Map;

public class CoordinatorAgent extends Agent {
    private CityMap cityMap;
    private Map<String, String> agentLocations = new HashMap<>();

    protected void setup() {
        cityMap = new CityMap();
        initializeMap(); // Crear el grafo con ubicaciones y distancias
        addBehaviour(new CollectLocationsBehaviour());
        addBehaviour(new ReceiveArrivalConfirmationBehaviour());
    }

    private void initializeMap() {
        cityMap.addLocation("A");
        cityMap.addLocation("B");
        cityMap.addLocation("C");
        cityMap.addRoad("A", "B", 5);
        cityMap.addRoad("B", "C", 10);
        cityMap.addRoad("A", "C", 15);
    }

    private class CollectLocationsBehaviour extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                System.out.println("Received location from " + msg.getSender().getLocalName() + ": " + msg.getContent());
                String sender = msg.getSender().getLocalName();
                String location = msg.getContent();


                agentLocations.put(sender, location);
                System.out.println("Received location from " + sender + ": " + location);
                System.out.println("++Number of agents: " + agentLocations.size() );

                    // Solo agrega la ubicación si el agente aún no ha sido registrado
                if (!agentLocations.containsKey(sender)) {
                    agentLocations.put(sender, location);
                    System.out.println("Received location from " + sender + ": " + location);
                    System.out.println("++Number of agents: " + agentLocations.size());
                } else {
                    System.out.println("Duplicate message from: " + sender + ", ignoring...");
                }

                if (agentLocations.size() == 2) { // Cambia este número según el número de agentes
                    System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++ ");

                    String optimalLocation = calculateOptimalMeetingPlace();
                    System.out.println("Optimal Meeting Location: " + optimalLocation);

                    ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
                    for (String agent : agentLocations.keySet()) {
                        inform.addReceiver(new AID(agent, AID.ISLOCALNAME));
                    }
                    inform.setContent(optimalLocation);
                    send(inform);
                    System.out.println("Meeting Point sent to agents: " + optimalLocation);

                } else {
                    block();
                }

            } else {
                block();
            }
        }

        private String calculateOptimalMeetingPlace() {
            Graph<String, DefaultWeightedEdge> graph = cityMap.getGraph();
            DijkstraShortestPath<String, DefaultWeightedEdge> dijkstra = new DijkstraShortestPath<>(graph);

            String optimalLocation = null;
            double minTotalDistance = Double.MAX_VALUE;

            for (String location : graph.vertexSet()) {
                double totalDistance = 0;
                for (String agentLocation : agentLocations.values()) {
                    totalDistance += dijkstra.getPathWeight(agentLocation, location);
                }
                if (totalDistance < minTotalDistance) {
                    minTotalDistance = totalDistance;
                    optimalLocation = location;
                }
            }
            System.out.println("Optimal Meeting Location: " + optimalLocation);

            return optimalLocation;
        }
    }

    private class ReceiveArrivalConfirmationBehaviour extends CyclicBehaviour {
        private int agentsArrived = 0;
    
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                if (msg.getContent().equals("ARRIVED")) {
                    agentsArrived++;
                    System.out.println(msg.getSender().getLocalName() + " has arrived at the meeting point.");
    
                    if (agentsArrived == agentLocations.size()) {
                        System.out.println("All agents have arrived at the meeting point!");
                    }
                }
            } else {
                block();
            }
        }
    }
    
}
