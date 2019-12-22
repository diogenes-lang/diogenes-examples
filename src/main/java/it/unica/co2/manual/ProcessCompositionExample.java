package it.unica.co2.manual;

import static it.unica.co2.api.contract.utils.ContractFactory.externalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.internalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.recRef;
import static it.unica.co2.api.contract.utils.ContractFactory.recursion;

import co2api.ContractException;
import co2api.Message;
import co2api.Session;
import it.unica.co2.api.contract.Recursion;
import it.unica.co2.api.contract.SessionType;
import it.unica.co2.api.process.Participant;

public class ProcessCompositionExample {

	private static String user = "alice@test.com";
	private static String pass = "alice";

	public static class ProcessA extends Participant {

		private static final long serialVersionUID = 1L;

		private final Session<SessionType> session;
		
		protected ProcessA(Session<SessionType> session) {
			super(user, pass);
			this.session = session;
		}

		@Override
		public void run() {
			session.sendIfAllowed("a");
			
			processCall(ProcessA.class, session);
		}
	}
	
	public static class ProcessB extends Participant {

		private static final long serialVersionUID = 1L;

		private final Session<SessionType> session;
		
		protected ProcessB(Session<SessionType> session) {
			super(user, pass);
			this.session = session;
		}

		@Override
		public void run() {
			session.sendIfAllowed("b");
		}
	}
	
	public static class ComposedProcess extends Participant {

		private static final long serialVersionUID = 1L;
		
		public ComposedProcess() {
			super(user, pass);
		}

		@Override
		public void run() {
			
			Recursion rec = recursion("x");
			rec.setContract(internalSum().add("a", recRef(rec)));
			SessionType C = externalSum().add("request", internalSum().add("a", rec).add("b"));
			
			Session<SessionType> session = tellAndWait(C);

			Message msg = null;
			
			msg = session.waitForReceive("request");

			int n;
			try {
				n = Integer.valueOf(msg.getStringValue());
			}
			catch (Throwable e) {
				n=0;
			}
			
			if (n>=0)
				processCall(ProcessA.class, session);
			else
				processCall(ProcessB.class, session);
				
//			session.sendIfAllowed("end");		//JPF fails
			
		}

	}
	
	
	public static void main(String[] args) throws ContractException {
		new ComposedProcess().run();
	}
}
