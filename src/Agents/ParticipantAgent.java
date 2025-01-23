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

import java.util.HashMap;
import java.util.Map;

public class ParticipantAgent extends Agent {
    private String location; // Current location of the agent
    private CityMap cityMap; // Shared city map
    private Map<String, String> agentLocations = new HashMap<>(); // Map to store locations of all agents
    private int expectedAgents = 3; // Total number of agents (including itself)

    @Override
    protected void setup() {
        // Initialize the agent's location from arguments
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            location = (String) args[0];
        } else {
            System.out.println(getLocalName() + ": No location specified!");
            doDelete();
            return;
        }

        // Initialize the city map
        cityMap = CityMap.getInstance(20, 10);

        // Store its own location in the map
        agentLocations.put(getLocalName(), location);

        // Add behaviours
        addBehaviour(new BroadcastLocationBehaviour());
        addBehaviour(new ReceiveLocationBehaviour());

        // Debugging information
        System.out.println("Expected agents: " + expectedAgents);
        System.out.println("Current agent locations: " + agentLocations);
        System.out.println("Graph vertices: " + cityMap.getGraph().vertexSet());
        System.out.println("Graph edges: " + cityMap.getGraph().edgeSet());
    }

    /**
     * Behaviour to broadcast the agent's location to others after a 3-second delay.
     */
    private class BroadcastLocationBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            try {
                // Delay of 3 seconds before sending the location
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setContent(location);

            // Send the message to all other agents
            for (int i = 1; i <= expectedAgents; i++) {
                String agentName = "participant" + i; // Assuming agent names like participant1, participant2, etc.
                if (!agentName.equals(getLocalName())) { // Exclude itself
                    msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
                }
            }

            send(msg);
            System.out.println(getLocalName() + " sent location: " + location);
        }
    }

    /**
     * Behaviour to receive and store other agents' locations.
     */
    private class ReceiveLocationBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                String sender = msg.getSender().getLocalName();
                String receivedLocation = msg.getContent();

                // Save the received location if not already present
                if (!agentLocations.containsKey(sender)) {
                    agentLocations.put(sender, receivedLocation);
                    System.out.println(getLocalName() + " received location from " + sender + ": " + receivedLocation);
                    System.out.println(getLocalName() + " has locations: " + agentLocations);

                    // Calculate the meeting point once all locations are received
                    if (agentLocations.size() == expectedAgents) {
                        calculateOptimalMeetingPlace();
                    }
                }
            } else {
                block(); // Wait for more messages
            }
        }

        /**
         * Calculate the optimal meeting place based on the shortest path distances.
         */
        private void calculateOptimalMeetingPlace() {
            Graph<String, DefaultWeightedEdge> graph = cityMap.getGraph();
            FloydWarshallShortestPaths<String, DefaultWeightedEdge> floydWarshall =
                    new FloydWarshallShortestPaths<>(graph);

            // Validate agent locations
            for (String agentLocation : agentLocations.values()) {
                if (!graph.containsVertex(agentLocation)) {
                    System.out.println("Error: Location " + agentLocation + " not found in the graph.");
                    return;
                }
            }

            HashMap<String, Double> totalCosts = new HashMap<>();
            HashMap<String, Double> maxCosts = new HashMap<>();

            // Calculate distances for each node
            for (String location : graph.vertexSet()) {
                double totalDistance = 0;
                double maxDistance = 0;

                for (String agentLocation : agentLocations.values()) {
                    double distance = floydWarshall.getPathWeight(agentLocation, location);

                    if (distance != Double.POSITIVE_INFINITY) { // Only consider reachable locations
                        totalDistance += distance;
                        maxDistance = Math.max(maxDistance, distance); // Track the maximum distance
                    }
                }

                totalCosts.put(location, totalDistance);
                maxCosts.put(location, maxDistance);
            }

            // Find the optimal node (minimum total distance, then minimum maximum distance)
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
