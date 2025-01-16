package Agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import Models.CityMap;

import java.util.List;

public class ParticipantAgent extends Agent {
    private String location; // Ubicación actual
    private CityMap cityMap; // Mapa compartido con el CoordinatorAgent

    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            location = (String) args[0];
        } else {
            System.out.println(getLocalName() + ": No location specified!");
            doDelete();
        }

        cityMap = new CityMap();
        initializeMap();

        // Envía la ubicación inicial al CoordinatorAgent
        addBehaviour(new SendInitialLocationBehaviour());

        // Recibe la ubicación óptima y se mueve hacia ella
        addBehaviour(new ReceiveOptimalLocationBehaviour());
    }

    private void initializeMap() {
        cityMap.addLocation("A");
        cityMap.addLocation("B");
        cityMap.addLocation("C");
        cityMap.addRoad("A", "B", 5);
        cityMap.addRoad("B", "C", 10);
        cityMap.addRoad("A", "C", 15);
    }

    private class SendInitialLocationBehaviour extends OneShotBehaviour {
        public void action() {
            try {
                // Retraso de 3 segundos antes de enviar la ubicación
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        
            AID coordinator = new AID("coordinator", AID.ISLOCALNAME);
        
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(coordinator);
            msg.setContent(location);
            send(msg);
            System.out.println(getLocalName() + " sent initial location: " + location);
        
        }
    }

    private class ReceiveOptimalLocationBehaviour extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                String optimalLocation = msg.getContent();
                System.out.println(getLocalName() + " received optimal location: " + optimalLocation);

                // Simula el movimiento hacia la ubicación óptima
                addBehaviour(new MoveToOptimalLocationBehaviour(optimalLocation));
            } else {
                block();
            }
        }
    }

    private class MoveToOptimalLocationBehaviour extends CyclicBehaviour {
        private String targetLocation;
        private List<String> path;

        public MoveToOptimalLocationBehaviour(String targetLocation) {
            this.targetLocation = targetLocation;

            // Calcula el camino más corto hacia el objetivo
            DijkstraShortestPath<String, DefaultWeightedEdge> dijkstra =
                    new DijkstraShortestPath<>(cityMap.getGraph());
            path = dijkstra.getPath(location, targetLocation).getVertexList();
        }

        public void action() {
            if (path.size() > 1) {
                // Mueve al siguiente nodo en el camino
                location = path.remove(1);
                System.out.println(getLocalName() + " moved to: " + location);

                // Simula tiempo de movimiento
                block(1000); // Espera 1 segundo
            } else {
                System.out.println(getLocalName() + " reached the optimal location: " + targetLocation);

                // Envía confirmación al CoordinatorAgent
                ACLMessage arrivedMsg = new ACLMessage(ACLMessage.INFORM);
                arrivedMsg.addReceiver(new AID("coordinator", AID.ISLOCALNAME));
                arrivedMsg.setContent("ARRIVED");
                send(arrivedMsg);

                removeBehaviour(this); // Termina el comportamiento
            }
        }
    }
}
