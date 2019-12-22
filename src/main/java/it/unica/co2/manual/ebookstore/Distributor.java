package it.unica.co2.manual.ebookstore;

import static it.unica.co2.api.contract.utils.ContractFactory.externalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.internalSum;

import co2api.ContractException;
import co2api.Message;
import co2api.Session;
import it.unica.co2.api.contract.SessionType;
import it.unica.co2.api.process.Participant;

public class Distributor extends Participant {
	
	private static final long serialVersionUID = 1L;
    private static String username = "testuser3@gmail.com";
    private static String password = "testuser3";
	
	public Distributor() {
		super(username, password);
	}
	
	@Override
	public void run() {
		
		SessionType c = externalSum().add(
				"bookdistrib",
				internalSum()
					.add("confirmdistr", externalSum().add("paydistrib").add("quitdistr"))
					.add("abortdistrib")
		);
		
		Session<SessionType> session = tellAndWait(c);
		
		Message msg;	
		String isbn;
		

		try {
			msg = session.waitForReceive("bookdistrib");
			isbn = msg.getStringValue();
			
			if (isPresent(isbn)) {
				session.sendIfAllowed("confirmdistr");
				
				msg = session.waitForReceive("paydistrib", "quitdistr");
				
				switch (msg.getLabel()) {
				
				case "paydistrib":	
					logger.info("pay received");
					break;
				
				case "quitdistr": 
					logger.info("quit received");
					break;
				}
			}
			else {
				session.sendIfAllowed("abortdistrib");
			} 
			
		}
		catch (ContractException e) {
			logger.info("exception: "+e.getMessage());
		}
		
	}

	public static void main(String args[]) throws ContractException {
		new Distributor().run();
	}

	private static boolean isPresent(String isbn) {
	
		return true;
	}


}
