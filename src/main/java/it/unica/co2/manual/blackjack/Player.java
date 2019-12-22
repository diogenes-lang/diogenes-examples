package it.unica.co2.manual.blackjack;

import static it.unica.co2.api.contract.utils.ContractFactory.externalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.internalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.recRef;
import static it.unica.co2.api.contract.utils.ContractFactory.recursion;

import co2api.ContractException;
import co2api.Message;
import co2api.Session;
import it.unica.co2.api.contract.Recursion;
import it.unica.co2.api.contract.SessionType;
import it.unica.co2.api.process.CO2Process;
import it.unica.co2.api.process.Participant;

public class Player extends Participant {
	
	private static final long serialVersionUID = 1L;
	private static String username = "nicola.a@test.com";
	private static String password = "cicciolina";
	
	public Player() {
		super(username, password);
	}

	@Override
	public void run() {

		Recursion contract = recursion("x");
		
		SessionType hit = externalSum().add("card", recRef(contract)).add("lose").add("abort");
		SessionType end = externalSum().add("win").add("lose").add("abort");
		
		contract.setContract(internalSum().add("hit", hit).add("stand", end));
	
		Session<SessionType> session = tellAndWait(contract);
		
		processCall(Play.class, session, 0);
	}

	private static class Play extends CO2Process {

		private static final long serialVersionUID = 1L;
		
		private final Session<SessionType> session;
		private final Integer n;
		
		protected Play(Session<SessionType> session, Integer n) {
			super();
			this.session = session;
			this.n=n;
		}

		@Override
		public void run() {
			
			Message msg;
			
			if (n<21) {
				session.sendIfAllowed("hit");
				
				msg = session.waitForReceive("card", "lose", "abort");
				
				switch(msg.getLabel()) {
				case "card":
					System.out.println("card received");
					try {
						Integer n = Integer.parseInt(msg.getStringValue());
						processCall(Play.class, session, this.n+n);
					}
					catch (NumberFormatException | ContractException e) {
						throw new RuntimeException(e);
					}
					break;
				
				case "lose":
					System.out.println("you lose! :(");
					break;
					
				case "abort":
					System.out.println("abort by the dealer");
					break;
				}
				
				
			}
			else {
				session.sendIfAllowed("stand");

				msg = session.waitForReceive("win", "lose", "abort");
				
				switch(msg.getLabel()) {
				case "win":
					System.out.println("you win! :)");
					break;
					
				case "lose":
					System.out.println("you lose! :(");
					break;
				
				case "abort":
					System.out.println("abort by the dealer");
					break;
				}
			}
			
			
			
		}
	}
	
	public static void main(String args[]) {
		new Player().run();
	}
}
