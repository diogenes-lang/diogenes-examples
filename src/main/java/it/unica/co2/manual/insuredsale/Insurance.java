package it.unica.co2.manual.insuredsale;

import static it.unica.co2.api.contract.utils.ContractFactory.externalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.internalSum;

import java.io.FileNotFoundException;

import co2api.ContractException;
import co2api.ContractExpiredException;
import co2api.Message;
import co2api.Session;
import it.unica.co2.api.contract.SessionType;
import it.unica.co2.api.process.Participant;
import it.unica.co2.api.process.SkipMethod;
import it.unica.co2.honesty.HonestyChecker;

public class Insurance extends Participant {

	private static final long serialVersionUID = 1L;

	public Insurance() {
		super("insurance@nicola.com", "insurance");
	}

	@Override
	public void run() {

		SessionType CI = externalSum().add("req", internalSum().add("ok").add("abort"));
		
		Session<SessionType> session = tellAndWait(CI);
		
		Message msg = session.waitForReceive("req");
		
		System.out.println("OOOOOOOOOOOOOOOOOOOOOOOOO-"+isVoid());
		
		Integer amount;
		try {
			amount = Integer.parseInt(msg.getStringValue());
		}
		catch (NumberFormatException | ContractException e) {
			throw new RuntimeException(e);
		}
		
		try {
			if (isInsurable(amount)) {
				session.sendIfAllowed("ok");
			}
			else {
				session.sendIfAllowed("abort");
			}
		}
		catch (Exception e) {
			session.sendIfAllowed("abort");
		}
	}

	@SkipMethod("true")
	private boolean isInsurable(Integer amount) throws RuntimeException, FileNotFoundException {
		
		throw new ContractExpiredException("");
		
//		return amount < 100;
	}
	
	@SkipMethod("foo")
	private String[] isVoid() {
		return new String[]{"pippo"};
	}
	
	public static void main(String[] args) {
		HonestyChecker.isHonest(Insurance.class);
	}
}
