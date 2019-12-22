package it.unica.co2.manual;

import static it.unica.co2.api.contract.utils.ContractFactory.externalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.recRef;
import static it.unica.co2.api.contract.utils.ContractFactory.recursion;

import co2api.ContractException;
import co2api.Message;
import co2api.Session;
import it.unica.co2.api.contract.Recursion;
import it.unica.co2.api.contract.SessionType;
import it.unica.co2.api.process.CO2Process;
import it.unica.co2.api.process.Participant;

public class ProcessComposition2Example {

	public static class Composed2Process extends Participant {

		private static final long serialVersionUID = 1L;
		
		private static String username = "alice@test.com";
		private static String password = "alice";

		public Composed2Process() {
			super(username, password);
		}

		@Override
		public void run() {
			
			Recursion rec = recursion("x");
			rec.setContract(externalSum().add("a", recRef(rec)).add("b", recRef(rec)));
			
			SessionType C = externalSum().add("request", rec);
			
			Session<SessionType> session = tellAndWait(C);

			Message msg = null;
			
			msg = session.waitForReceive("request");

			@SuppressWarnings("unused")
			int n;
			try {
				n = Integer.valueOf(msg.getStringValue());
			}
			catch (Throwable e) {
				n=0;
			}
			
			processCall(ProcessA.class, session);
			
//			session.sendIfAllowed("end");		//JPF fails
			
		}

	}

	
	public static class ProcessA extends CO2Process {

		private static final long serialVersionUID = 1L;

		private final Session<SessionType> session;
		
		protected ProcessA(Session<SessionType> session) {
			super();
			this.session = session;
		}

		@Override
		public void run() {
			session.waitForReceive("a", "b");
			
			processCall(ProcessA.class, session);
		}
	}
	
	
	public static void main(String[] args) throws ContractException {
		new Composed2Process().run();
	}
}
