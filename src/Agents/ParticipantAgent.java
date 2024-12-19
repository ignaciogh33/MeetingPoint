package Agents;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class ParticipantAgent extends Agent {
    private String location;

    protected void setup() {
        location = (String) getArguments()[0]; // Agent's initial location

        addBehaviour(new SendLocationBehaviour());
    }

    private class SendLocationBehaviour extends OneShotBehaviour {
        public void action() {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(getAID("coordinator")); // Replace with actual coordinator's name
            msg.setContent(location);
            send(msg);
        }
    }
}
