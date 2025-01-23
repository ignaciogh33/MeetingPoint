package Agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths;
import org.jgrapht.graph.DefaultWeightedEdge;
import Models.CityMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
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
    private Map<String, Double> costs = new HashMap<>();

    protected void setup() {

        // Inicializa el mapa de la ciudad
        cityMap = CityMap.getInstance(20, 40);

        List<String> vertices = new ArrayList<>(cityMap.getGraph().vertexSet());
        location = vertices.get((int) Math.floor(Math.random() * vertices.size()));
        // Guarda su propia ubicación en el mapa
        agentLocations.put(getLocalName(), location);

        // Comportamientos
        addBehaviour(new BroadcastLocationBehaviour());
        addBehaviour(new ReceiveLocationBehaviour());
        System.out.println("Current agent locations: " + agentLocations);

        if (getLocalName().contains("1")) {
            System.out.println("Expected agents: " + expectedAgents);
            System.out.println("Graph vertices: " + cityMap.getGraph().vertexSet());
            for (DefaultWeightedEdge edge: cityMap.getGraph().edgeSet()) {
                double weight = cityMap.getGraph().getEdgeWeight(edge);
                String source = cityMap.getGraph().getEdgeSource(edge);
                String target = cityMap.getGraph().getEdgeTarget(edge);


                System.out.println(source + " - " + target + ": " + weight);
            }
            System.out.println("Graph edges: " + cityMap.getGraph().edgeSet());
        }
        computeShortestPathToEachvertex();
    }

    private void computeShortestPathToEachvertex() {
        DijkstraShortestPath<String, DefaultWeightedEdge> dijkstraAlg = new DijkstraShortestPath<>(cityMap.getGraph());

        for (String vertex : cityMap.getGraph().vertexSet()) {
            costs.put(vertex, dijkstraAlg.getPathWeight(location, vertex));
        }

    }

    private class BroadcastLocationBehaviour extends OneShotBehaviour {

        public void action() {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            try {
                msg.setContentObject((Serializable) costs);
                for (int i = 1; i <= expectedAgents; i++) {
                    String agentName = "participant" + i;
                    if (!agentName.equals(getLocalName())) {
                        msg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
                    }
                }

                send(msg);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

    private class ReceiveLocationBehaviour extends CyclicBehaviour {

        int nbReceived = 1;

        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                nbReceived++;

                try {
                    Map<String, Double> receivedCosts = (HashMap<String, Double>) msg.getContentObject();
                    for (String key : receivedCosts.keySet()) {
                        costs.put(key, costs.get(key) + receivedCosts.get(key));
                    }
                } catch (UnreadableException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (nbReceived == expectedAgents) {
                    calculateOptimalMeetingPlace();
                }
            } else {
                block();
            }
        }

        private void calculateOptimalMeetingPlace() {
            double minCost = Double.MAX_VALUE;
            String optimalLocation = "";
            if (getLocalName().contains("1")) {
                StringBuilder sb = new StringBuilder();
                for (String key : costs.keySet()) {
                    sb.append(key + ": " + costs.get(key) + "\n");
                }
                System.out.println(sb.toString());
            }

            for (String key : costs.keySet()) {

                if (costs.get(key) < minCost) {
                    minCost = Math.min(minCost, costs.get(key));
                    optimalLocation = key;
                }
            }
            System.out.println(getLocalName() + " calculated optimal meeting location: " + optimalLocation
                    + " with total path weight: " + minCost);
        }

    }
}
