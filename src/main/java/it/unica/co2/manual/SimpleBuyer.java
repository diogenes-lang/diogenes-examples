package it.unica.co2.manual;

import static it.unica.co2.api.contract.utils.ContractFactory.externalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.internalSum;

import co2api.ContractException;
import co2api.Message;
import co2api.Session;
import it.unica.co2.api.contract.SessionType;
import it.unica.co2.api.process.Participant;
import it.unica.co2.honesty.HonestyChecker;

public class SimpleBuyer extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static final String username = "alice@test.com";
	private static final String password = "alice";
	
	public SimpleBuyer() {
		super(username, password);
	}

	
	private SessionType contract = 
			internalSum().add(
					"item", //Sort.STRING, 
					externalSum().add("amount", //Sort.INT, 
							internalSum()
							.add("pay", /*Sort.INT,*/ externalSum().add("item"))
							.add("abort")
							)
			);
	
	@Override
	public void run() {
		
		try {
			Session<SessionType> session = tellAndWait(contract);
			
			session.sendIfAllowed("item", "01234");
			
			Message msg = session.waitForReceive("amount");
			
			Integer n = Integer.valueOf(msg.getStringValue());
			
			if (n<10) {
				session.sendIfAllowed("pay", n);
				msg = session.waitForReceive("item");
			}
			else {
				session.sendIfAllowed("abort");
			}
		}
		catch (ContractException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		HonestyChecker.isHonest(SimpleBuyer.class);
	}

}
