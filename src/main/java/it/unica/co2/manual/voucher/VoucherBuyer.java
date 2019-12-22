package it.unica.co2.manual.voucher;

import static it.unica.co2.api.contract.utils.ContractFactory.externalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.internalSum;

import co2api.Message;
import co2api.Session;
import it.unica.co2.api.contract.SessionType;
import it.unica.co2.api.process.Participant;

public class VoucherBuyer extends Participant {

	private static final long serialVersionUID = 1L;
	
	private static String username = "voucher.buyer@nicola.com";
	private static String password = "voucher.buyer";
	
	public VoucherBuyer() {
		super(username, password);
	}

	@Override
	public void run() {
		
		SessionType Cvoucher = externalSum()
				.add("reject", internalSum().add("pay"))
				.add("accept", internalSum().add("voucher"));
		
		SessionType CB = internalSum()
				.add("clickpay", internalSum().add("pay"))
				.add("clickvoucher", Cvoucher);
		
		
		Session<SessionType> session = tellAndWait(CB);
		
		if (useVoucher()) {
			
			session.sendIfAllowed("clickvoucher");

			Message msg = session.waitForReceive("reject", "accept");
			
			switch(msg.getLabel()) {
			
			case "reject":
				session.sendIfAllowed("pay");
				break;
				
			case "accept":
				session.sendIfAllowed("voucher");
				break;
			}
			
		}
		else {
			session.sendIfAllowed("clickpay");
			session.sendIfAllowed("pay");
		}
		
		
		
	}

	private boolean useVoucher() {
		return true; //new Random().nextBoolean();
	}
	
	public static void main(String[] args) {
		new VoucherBuyer().run();
	}
}
