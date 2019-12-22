package it.unica.co2.manual.insuredsale;

import static it.unica.co2.api.contract.utils.ContractFactory.externalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.internalSum;

import co2api.ContractException;
import co2api.Message;
import co2api.Public;
import co2api.Session;
import co2api.TimeExpiredException;
import it.unica.co2.api.contract.SessionType;
import it.unica.co2.api.process.Participant;

public class InsuredSeller extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static String username = "insuredseller1@nicola.com";
	private static String password = "insuredseller1";
	
	public InsuredSeller() {
		super(username, password);
	}


	
	@Override
	public void run() {
		
		SessionType CA = externalSum().add("order", 
				internalSum()
				.add("amount", externalSum().add("pay"))
				.add("abort"));
		
		
		Public<SessionType> pbl = tell(CA);
		Session<SessionType> session = pbl.waitForSession();
		
		Message msg = session.waitForReceive("order");
		
		try {
			Integer n = Integer.parseInt(msg.getStringValue());
			
			if (n<50) {
				processCall(HandlePayment.class, session, n);
			}
			else {
				processCall(HandleInsurance.class, session, n);
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}

	private static class HandlePayment extends Participant {

		private static final long serialVersionUID = 1L;
		private final Session<SessionType> session;
		private final Integer amount;
		
		protected HandlePayment(Session<SessionType> session, Integer amount) {
			super(InsuredSeller.username, InsuredSeller.password);
			this.session = session;
			this.amount = amount;
		}

		@Override
		public void run() {
			
			if (isServiceAvailable()) {
				session.sendIfAllowed("amount", amount);
				Message msg = session.waitForReceive("pay");
				
				try {
					String payment = msg.getStringValue();
					handlePayment(payment);
				}
				catch (ContractException e) {
					throw new RuntimeException(e);
				}
				
			}
			else {
				session.sendIfAllowed("abort");
			}
		}
	
		
		
		private boolean isServiceAvailable() {
			return true;
		}
		
		private void handlePayment(String payment) {}
	}
	
	
	private static class HandleInsurance extends Participant {

		private static final long serialVersionUID = 1L;
		private final Session<SessionType> session;
		private final Integer amount;
		
		protected HandleInsurance(Session<SessionType> session, Integer amount) {
			super(InsuredSeller.username, InsuredSeller.password);
			this.session = session;
			this.amount = amount;
		}

		@Override
		public void run() {
			
			SessionType CI = internalSum().add("reqi", externalSum().add("oki").add("aborti"));

			Public<SessionType> pblI = tell(CI);
		
			Session<SessionType> sessionI;
			
			try {
				sessionI = pblI.waitForSession(10000);
				
				sessionI.sendIfAllowed("reqi", amount);
				
				try {
					Message msg = sessionI.waitForReceive(10000, "oki", "aborti");
					
					switch (msg.getLabel()) {
					
					case "oki":
						processCall(HandlePayment.class, session, amount);
						break;
						
					case "aborti":
						session.sendIfAllowed("abort");
						break;
					}
					
				}
				catch (TimeExpiredException e) {
					
					session.sendIfAllowed("abort");
					
					sessionI.waitForReceive("oki", "aborti");
				}
				
			}
			catch (TimeExpiredException e) {
				
				session.sendIfAllowed("abort");
				
				sessionI = pblI.waitForSession();
				sessionI.sendIfAllowed("reqi");
				sessionI.waitForReceive("oki", "aborti");
			}
		
		}
	
	}
	
	public static void main(String[] args) {
		new InsuredSeller().run();
	}
}
