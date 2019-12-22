package it.unica.co2.manual;

import co2api.CO2ServerConnection;
import co2api.ContractException;
import co2api.ContractViolationException;
import co2api.Message;
import co2api.Public;
import co2api.Session;
import co2api.TST;

public class Buyer {

	public static void main(String args[]) throws ContractException {

		String storeContractID = "";
		
		CO2ServerConnection co2 = new CO2ServerConnection("buyer@gmail.com", "buyer");

		Public<TST> pB = Public.accept(co2, storeContractID, TST.class);
		Session<TST> sB = pB.waitForSession();
		
		String order = "8794567347859"; // book id
		Integer desiredPrice = 10;

		sB.send("order", order);

		try {
			Message mB = sB.waitForReceive();

			switch (mB.getLabel()) {
			case "unavailable":
				break;
				
			case "price":
				Integer price = Integer.parseInt(mB.getStringValue());
				if (price > desiredPrice) {
					// 
				}
				else {
					//
				}
			}
		} 
		catch (ContractViolationException e) {
			System.out.println("The store is culpable.");
		}
	}
}
