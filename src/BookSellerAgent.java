package jadelab2;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;




public class BookSellerAgent extends Agent {
  private Hashtable<String, List<BookCopy>> catalogue;
  private BookSellerGui myGui;

  protected void setup() {
    catalogue = new Hashtable();
    myGui = new BookSellerGui(this);
    myGui.display();

    //book selling service registration at DF
    DFAgentDescription dfd = new DFAgentDescription();
    dfd.setName(getAID());
    ServiceDescription sd = new ServiceDescription();
    sd.setType("book-selling");
    sd.setName("JADE-book-trading");
    dfd.addServices(sd);
    try {
      DFService.register(this, dfd);
    }
    catch (FIPAException fe) {
      fe.printStackTrace();
    }
    
    addBehaviour(new OfferRequestsServer());

    addBehaviour(new PurchaseOrdersServer());
  }

  protected void takeDown() {
    //book selling service deregistration at DF
    try {
      DFService.deregister(this);
    }
    catch (FIPAException fe) {
      fe.printStackTrace();
    }
  	myGui.dispose();
    System.out.println("Seller agent " + getAID().getName() + " terminated.");
  }

  //invoked from GUI, when a new book is added to the catalogue
  public void updateCatalogue(final String title, final int price, final int ship) {
    addBehaviour(new OneShotBehaviour() {
      public void action() {
		Integer total = price + ship;

		List<BookCopy> bookList = catalogue.get(title);

		if (bookList != null){
			int insertIndex = 0;
            while (insertIndex < bookList.size() && bookList.get(insertIndex).price <= total) {
                insertIndex++;
            }

			bookList.add(insertIndex, new BookCopy(total, title + "-" + price + "-" + bookList.size()));

		} else {
			List<BookCopy> copies = new ArrayList<BookCopy>();
			copies.add(new BookCopy(total, title + "-" + price + "-" + "0"));
			catalogue.put(title, copies);
		}
		System.out.println(getAID().getLocalName() + ": " + title + " put into the catalogue. Price = " +  price + ", Shipping Cost = " + ship + ", Total = " + (price + ship));
      }
    } );
  }
  
	private class OfferRequestsServer extends CyclicBehaviour {
		public void action() {
			//proposals only template
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {

			String title = msg.getContent();
			ACLMessage reply = msg.createReply();
			List<BookCopy> copies = catalogue.get(title);

			if(copies != null){
				boolean found = false;
				for(BookCopy copy : copies) {
					if(!copy.reserved){
						copy.reserved = true;
						reply.setPerformative(ACLMessage.PROPOSE);
						reply.setContent(String.valueOf(copy.id));

						// Add a behaviour to release the reservation in 20 seconds
                        myAgent.addBehaviour(new WakerBehaviour(myAgent, 20000) {
                            protected void onWake() {
                                if (copy.reserved) {
                                    copy.reserved = false; // Liberar la reserva
                                    System.out.println("Reservation expired: " + copy.id);
                                }
                            }
                        });





						found = true;
						break;
					}
				}
				if (!found){
					//all copies reserved
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("no-copies-available");
				}

			} else {
				//title not found in the catalogue
				reply.setPerformative(ACLMessage.REFUSE);
				reply.setContent("not-available");
			}
			myAgent.send(reply);

			} else {
				block();
			}
		}
	}

	
	private class PurchaseOrdersServer extends CyclicBehaviour {
	  public void action() {
	    //purchase order as proposal acceptance only template
		MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
		ACLMessage msg = myAgent.receive(mt);
	    if (msg != null) {

	      String bookId = msg.getContent();
	      ACLMessage reply = msg.createReply();

		  String[] parts = bookId.split("-"); // Split the string at the '-'
		  String bookName = parts[0];         // First part is the book name
		  int price = Integer.parseInt(parts[1]); // Second part is the price, convert it to an integer


	      if (catalogue.get(bookName) != null) {
	        reply.setPerformative(ACLMessage.INFORM);

			if (catalogue.get(bookName).size() == 1){
				catalogue.remove(bookName);
			} else {
				catalogue.get(bookName).remove(0);
			}

	        System.out.println(getAID().getLocalName() + ": " + bookName + " sold to " + msg.getSender().getLocalName());
	      }
	      else {
	        //title not found in the catalogue, sold to another agent in the meantime (after proposal submission)
	        reply.setPerformative(ACLMessage.FAILURE);
	        reply.setContent("not-available");
	      }
	      myAgent.send(reply);
	    }
	    else {
		  block();
		}
	  }
	}

}

class BookCopy {
	int price;
	boolean reserved;
	String id;

	BookCopy(int price, String id) {
		this.price = price;
		this.reserved = false;
		this.id = id;
	}
}
