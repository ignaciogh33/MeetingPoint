package Agents;


import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jheaps.AddressableHeap;

import java.util.HashMap;
import java.util.Map;
import Models.CityMap;

public class CoordinatorAgent extends Agent {
    private CityMap cityMap;
    private Map<String, String> agentLocations = new HashMap<>();

    protected void setup() {
        System.out.println("CoordinatorAgent " + getLocalName() + " initialized.");
        cityMap = new CityMap(); // Initialize your city map here.

        // Add behavior to collect locations from agents
        addBehaviour(new CollectLocationsBehaviour());
    }

    private class CollectLocationsBehaviour extends OneShotBehaviour {
        public void action() {
            ACLMessage msg = receive();
            while (msg != null) {
                agentLocations.put(msg.getSender().getLocalName(), msg.getContent());
                msg = receive();
            }

            // Calculate optimal meeting location
            String optimalLocation = calculateOptimalMeetingPlace();
            System.out.println("Optimal Meeting Location: " + optimalLocation);

            // Inform agents
            ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
            for (String agent : agentLocations.keySet()) {
                inform.addReceiver(getAID());
            }
            inform.setContent(optimalLocation);
            send(inform);
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
            return optimalLocation;
        }
    }
}
