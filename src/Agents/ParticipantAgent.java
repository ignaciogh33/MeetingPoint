package Agents;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

public class ParticipantAgent extends Agent {
    private String location;

    protected void setup() {
        System.out.println("ParticipantAgent " + getLocalName() + " initialized with location: " + location);

        location = (String) getArguments()[0]; // Agent's initial location

        addBehaviour(new SendLocationBehaviour());
    }

    private class SendLocationBehaviour extends OneShotBehaviour {
        public void action() {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(new jade.core.AID("coordinator", jade.core.AID.ISLOCALNAME));
            msg.setContent(location);
            send(msg);
        }
    }
}
