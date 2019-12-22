package it.unica.co2.manual.travelagency;

import static it.unica.co2.api.contract.utils.ContractFactory.def;
import static it.unica.co2.api.contract.utils.ContractFactory.externalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.internalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.ref;

import co2api.ContractExpiredException;
import co2api.Message;
import co2api.Public;
import co2api.Session;
import co2api.TimeExpiredException;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.SessionType;
import it.unica.co2.api.contract.Sort;
import it.unica.co2.api.process.Participant;
import it.unica.co2.api.process.SkipMethod;
import it.unica.co2.api.process.SymbolicIf;
import it.unica.co2.honesty.HonestyChecker;

public class TravelAgency extends Participant {

	private static final long serialVersionUID = 1L;

	protected TravelAgency(String username, String password) {
		super(username, password);
	}
	
	/*
	 * contracts declaration
	 */
	private static ContractDefinition Cu = def("Cu");
	private static ContractDefinition Cf = def("Cf");
	private static ContractDefinition Ch = def("Ch");
	private static ContractDefinition d = def("d");
	
	/*
	 * contracts initialization
	 */
	static {
		
		/*
		 * Cu = ?tripdets . ( ?budget . ( !quote . ( ?pay . ( !commit + !abort ) )
		 * 
		 * contract between the agency and the consumer
		 */
		Cu.setContract(
				externalSum()
				.add("tripdets", Sort.string(), 
						externalSum()
						.add("budget", Sort.integer(), 
								internalSum()
								.add("quote", Sort.integer(), 
										externalSum()
										.add("pay", 
												internalSum()
												.add("commit")
												.add("abort")))
								.add("abort"))));
		
		/*
		 * Cf = !flightdets . d
		 * 
		 * contract between the agency and the flying company
		 */
		Cf.setContract(
				internalSum()
				.add("flightdets", Sort.string(), ref(d)));
		
		/*
		 * Ch = !hoteldets . d
		 * 
		 * contract between the agency and the hotel reservation service
		 */
		Ch.setContract(
				internalSum()
				.add("hoteldets", Sort.string(), ref(d)));

		/*
		 * d = ?quote . ( !pay . ( ?confirm . ( !commit + !abort ) ) + !abort )
		 */
		d.setContract(
				externalSum()
				.add("quote", Sort.integer(), 
						internalSum()
						.add("pay", 
								externalSum()
								.add("confirm", 
										internalSum()
										.add("commit")
										.add("abort")))
						.add("abort")));
		
		
	}

	@Override
	public void run() {
		
		Message msg;
		
		try {
			/*
			 * advertise the contract to the consumers
			 */
			logger.info("advertising contract Cu to the consumers: {}", Cu.getContract().toTST());
			Session<SessionType> customerSession = tellAndWait(Cu, 120_000);

			logger.info("contract Cu fused");
			logger.info("contractID: {}", customerSession.getContractID());
			logger.info("sessionID: {}", customerSession.getSessionID());
			
			logger.info("waiting for tripdets");
			msg = customerSession.waitForReceive("tripdets");
//			assert msg.getLabel()=="tripdets";
			String tripDets = msg.getStringValue();
			
			logger.info("tripdets: {}", tripDets);
			
			logger.info("waiting for budget");
			msg = customerSession.waitForReceive("budget");
//			assert msg.getLabel()=="budget";
			int budget = Integer.parseInt(msg.getStringValue());

			logger.info("budget: {}", budget);
			
			logger.info("calling Negotiator");
			processCall(Negotiatior.class, username, password, customerSession, tripDets, budget);
		}
		catch (ContractExpiredException e) {
			logger.info("the contract Cu is expired");
		}
		
	}
	
	
	
	public static class Negotiatior extends Participant {

		private static final long serialVersionUID = 1L;

		private Session<SessionType> consumerSession;
		private String tripDets;
		private int budget;
		
		protected Negotiatior(String username, String password, Session<SessionType> consumerSession, String tripDets, Integer budget) {
			super(username, password);
			this.consumerSession = consumerSession;
			this.tripDets = tripDets;
			this.budget = budget;
		}

		@Override
		public void run() {
			
			logger.info("advertising contract Cf to the flying company");
			Public<SessionType> pblXf = tell(Cf);

			logger.info("advertising contract Ch to the hotel reservation service");
			Public<SessionType> pblXh = tell(Ch);
			
			parallel(()->{
				logger.info("[thread-{}] sending 'flightdets'", Thread.currentThread().getId());
				pblXf.sendIfAllowed("flightdets", tripDets);
			});
			
			parallel(()->{
				logger.info("[thread-{}] sending 'hoteldets'", Thread.currentThread().getId());
				pblXh.sendIfAllowed("hoteldets", tripDets);
			});
			
			parallel(()->{
				logger.info("calling QuoteManager");
				processCall(QuoteManager.class, username, password, consumerSession, pblXf, pblXh, budget);
			});
		}
		
	}
	
	public static class QuoteManager extends Participant {

		private static final long serialVersionUID = 1L;

		private Session<SessionType> consumerSession;
		private Public<SessionType> pblXf;
		private Public<SessionType> pblXh;
		private int budget;
		private int quoteSum = 0;
		
		protected QuoteManager(
				String username, 
				String password, 
				Session<SessionType> consumerSession, 
				Public<SessionType> pblXf, 
				Public<SessionType> pblXh,
				Integer budget) {
			
			super(username, password);
			this.consumerSession = consumerSession;
			this.pblXf = pblXf;
			this.pblXh = pblXh;
			this.budget = budget;
		}

		@Override
		public void run() {
			
			try {
				multipleSessionReceiver()
				.add(pblXf, "quote", this::step1)
				.add(pblXh, "quote", this::step1)
				.waitForReceive(120_000);
			}
			catch (TimeExpiredException e) {
				logger.debug("no one responds in time, aborting");
				processCall(Abort.class, username, password, consumerSession, pblXf, pblXh);
			}
			
		}
		
		@SymbolicIf
		private void step1(Message msg) {
			
			logger.debug("STEP 1");
			
			@SuppressWarnings("unchecked")
			Session<SessionType> session = (Session<SessionType>) msg.getSession();
			printWhoCameFirst(session);
			
//			assert msg.getLabel().equals("quote");
			
			int quoteValue = Integer.parseInt(msg.getStringValue());
			
			logger.debug("received quote: {}", quoteValue);
			
			quoteSum+=quoteValue;
			
			logger.debug("quoteSum/budget: {}/{}", quoteSum, budget);
				
			if (quoteSum<budget) {
				
				try {
					multipleSessionReceiver()
						.add(pblXf, "quote", this::step2)
						.add(pblXh, "quote", this::step2)
						.waitForReceive(60_000);
				}
				catch (TimeExpiredException e) {
					processCall(Abort.class, username, password, consumerSession, pblXf, pblXh);
				}
			}
			else {
				processCall(Abort.class, username, password, consumerSession, pblXf, pblXh);
			}
			
		}

		@SymbolicIf
		private void step2(Message msg) {
			logger.debug("STEP 2");
			
			@SuppressWarnings("unchecked")
			Session<SessionType> session = (Session<SessionType>) msg.getSession();
			printWhoCameFirst(session);
			
//			assert msg.getLabel().equals("quote");
			
			int quoteValue = Integer.parseInt(msg.getStringValue());
			
			logger.debug("received quote: {}", quoteValue);
			
			quoteSum+=quoteValue;
			
			logger.debug("both quotes were received");
			logger.debug("quoteSum/budget: {}/{}", quoteSum, budget);
			
			if (quoteSum<budget) {
				//pay
				processCall(Pay.class, username, password, consumerSession, pblXf, pblXh, quoteSum);
			}
			else {
				//abort
				processCall(Abort.class, username, password, consumerSession, pblXf, pblXh);
			}
		}
		
		
		@SkipMethod
		private void printWhoCameFirst(Session<SessionType> session) {

			if (session.getContractID().equals(pblXf.getContractID())) {
				logger.debug("the flying company responds first");
			}
			else if (session.getContractID().equals(pblXh.getContractID())) {
				logger.debug("the hotel reservation service responds first");
			}
			else {
				throw new IllegalStateException("unexpected session with contract id '"+session.getContractID()+"'");
			}			
			
		}
		
	}
	
	
	public static class Abort extends Participant {

		private static final long serialVersionUID = 1L;

		private Session<SessionType> consumerSession;
		private Public<SessionType> pblXf;
		private Public<SessionType> pblXh;
		
		protected Abort(
				String username, 
				String password, 
				Session<SessionType> consumerSession, 
				Public<SessionType> pblXf, 
				Public<SessionType> pblXh) {
			
			super(username, password);
			this.consumerSession = consumerSession;
			this.pblXf = pblXf;
			this.pblXh = pblXh;
		}

		@Override
		public void run() {
			parallel(()->{consumerSession.sendIfAllowed("abort");});
			parallel(()->{pblXf.sendIfAllowed("abort");});
			parallel(()->{pblXh.sendIfAllowed("abort");});
			parallel(()->{consumerSession.waitForReceive("pay");});
			parallel(()->{pblXf.waitForReceive("quote");});
			parallel(()->{pblXh.waitForReceive("quote");});
			parallel(()->{pblXf.waitForReceive("confirm");});
			parallel(()->{pblXh.waitForReceive("confirm");});
		}
	}
	
	public static class Pay extends Participant {

		private static final long serialVersionUID = 1L;

		private Session<SessionType> consumerSession;
		private Public<SessionType> pblXf;
		private Public<SessionType> pblXh;
		private int amount;
		
		protected Pay(
				String username, 
				String password, 
				Session<SessionType> consumerSession, 
				Public<SessionType> pblXf, 
				Public<SessionType> pblXh,
				Integer amount) {
			
			super(username, password);
			this.consumerSession = consumerSession;
			this.pblXf = pblXf;
			this.pblXh = pblXh;
			this.amount = amount;
		}

		@Override
		public void run() {
			consumerSession.sendIfAllowed("quote", amount);
			
			try {
				consumerSession.waitForReceive(60_000, "pay");
				
				pblXf.sendIfAllowed("pay");
				pblXh.sendIfAllowed("pay");
				
				pblXf.waitForReceive(60_000, "confirm");
				pblXh.waitForReceive(60_000, "confirm");
				
				consumerSession.sendIfAllowed("commit");
				pblXf.sendIfAllowed("commit");
				pblXh.sendIfAllowed("commit");
				
			}
			catch (TimeExpiredException e) {
				processCall(Abort.class, username, password, consumerSession, pblXf, pblXh);
			}
		}
	}

	public static void main(String[] args) {
//		new TravelAgency("", "").run();
		HonestyChecker.isHonest(TravelAgency.class, "", "");
	}
}
