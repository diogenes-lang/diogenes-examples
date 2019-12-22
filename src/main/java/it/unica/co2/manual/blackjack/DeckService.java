package it.unica.co2.manual.blackjack;

import static it.unica.co2.api.contract.utils.ContractFactory.externalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.internalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.recRef;
import static it.unica.co2.api.contract.utils.ContractFactory.recursion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import co2api.Message;
import co2api.Session;
import it.unica.co2.api.contract.Recursion;
import it.unica.co2.api.contract.SessionType;
import it.unica.co2.api.process.CO2Process;
import it.unica.co2.api.process.Participant;

public class DeckService extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static final String username = "bob@test.com";
	private static final String password = "bob";
	
	
	public DeckService() {
		super(username, password);
	}
	
	@Override
	public void run() {
		
		Recursion contract = recursion("x");
		contract.setContract(externalSum().add("next", internalSum().add("card", recRef(contract))).add("abort"));

		System.out.println("tell and wait");
		Session<SessionType> session = tellAndWait(contract);
		
		processCall(Deck.class, session);
	}
	
	private static class Deck extends CO2Process {

		private static final long serialVersionUID = 1L;
		
		private final Session<SessionType> session;
		private List<Integer> deck = new ArrayList<>();
		
		protected Deck(Session<SessionType> session) {
			super();
			this.session = session;
			
			Integer[] cards = new Integer[]{1,2,3,4,5,6,7,8,9,10,10,10,10};
			
			deck.addAll(Arrays.asList(cards));
			deck.addAll(Arrays.asList(cards));
			deck.addAll(Arrays.asList(cards));
			deck.addAll(Arrays.asList(cards));
			
			assert deck.size()==52;
		}

		@Override
		public void run() {
			
			Message msg = session.waitForReceive("next", "abort");
			
			switch(msg.getLabel()) {
			case "next":
				session.sendIfAllowed("card", getNextCard());
				processCall(Deck.class, session);
				break;
				
			case "abort":
				break;
			}
			
		}

		private int getNextCard() {
			
			int size = deck.size();
			
			assert size>0: "your deck is ended";
			
			int choice = new Random().nextInt(size);
			int card = deck.remove(choice);
			
			System.out.println("extracted card "+card);
			
			assert deck.size()==size-1;
			
			return card;
		}
	}
	
	
	
	
	public static void main(String args[]) {
		new DeckService().run();
	}
}
