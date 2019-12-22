package it.unica.co2.manual.insuredsale;

import static it.unica.co2.api.contract.utils.ContractFactory.externalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.internalSum;

import co2api.Message;
import co2api.Session;
import it.unica.co2.api.contract.SessionType;
import it.unica.co2.api.process.Participant;

public class IBuyer extends Participant {

	private static final long serialVersionUID = 1L;

	public IBuyer() {
		super("buyer@nicola.com", "buyer");
	}

	@Override
	public void run() {
		SessionType CA = internalSum().add("order", 
				externalSum()
				.add("amount", internalSum().add("pay"))
				.add("abort"));
		
		
		Integer orderAmount = 60;
		
		Session<SessionType> session = tellAndWait(CA);
		
		session.sendIfAllowed("order", orderAmount);
		
		Message msg = session.waitForReceive("amount", "abort");
		
		switch(msg.getLabel()) {
		case "amount":
			session.sendIfAllowed("pay", "1234-0000-1234-0000");
			break;
			
		case "abort":
			break;
		}
		
	}

	public static void main (String[] args) {
		new IBuyer().run();
	}
}
