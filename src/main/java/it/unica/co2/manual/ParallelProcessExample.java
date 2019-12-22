package it.unica.co2.manual;

import static it.unica.co2.api.contract.utils.ContractFactory.externalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.internalSum;

import co2api.Session;
import it.unica.co2.api.contract.SessionType;
import it.unica.co2.api.process.Participant;

public class ParallelProcessExample {

	public static class ParallelProcess extends Participant {
		
		private static final long serialVersionUID = 1L;
		
		private static String username = "bob@test.com";
		private static String password = "bob";
		
		public ParallelProcess() {
			super(username, password);
		}

		@Override
		public void run() {
			
			SessionType C1 = internalSum().add("a", externalSum().add("a"));
			SessionType C2 = internalSum().add("b", externalSum().add("a"));
			SessionType C3 = internalSum().add("c", externalSum().add("a"));
			
			parallel(()-> {
				Session<SessionType> session = tellAndWait(C1);
				session.sendIfAllowed("a");
				session.waitForReceive("a","b");
			});
			
			parallel(()-> {
				Session<SessionType> session = tellAndWait(C2);
				session.sendIfAllowed("b");
				session.waitForReceive("a");
			});
			
			parallel(()-> {
				Session<SessionType> session = tellAndWait(C3);
				session.sendIfAllowed("c");
				session.waitForReceive("a");
			});
		}

	}
	
	public static void main(String[] args) {
		new ParallelProcess().run();
	}
	
}
