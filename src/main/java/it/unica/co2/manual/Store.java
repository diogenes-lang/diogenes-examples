package it.unica.co2.manual;

import static it.unica.co2.api.contract.utils.ContractFactory.externalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.internalSum;

import co2api.ContractExpiredException;
import co2api.Message;
import co2api.Public;
import co2api.Session;
import co2api.TimeExpiredException;
import it.unica.co2.api.contract.SessionType;
import it.unica.co2.api.process.Participant;
import it.unica.co2.honesty.HonestyChecker;

public class Store extends Participant {

	private static final long serialVersionUID = 1L;

	protected Store() {
		super("", "");
	}

	
	public void run(){

		SessionType b = externalSum().add("order",
								internalSum().add("price").add("unavailable"));
		
		SessionType d = internalSum().add("req",
				externalSum().add("ok").add("no"));

        Session<SessionType> sB = tellAndWait(b);
        String code = sB.waitForReceive("order").getStringValue();

        if (isAvailable(code)) { // handled internally
            sB.sendIfAllowed("price", getPrice(code));
        }
        else { // handled with the distributor
            Public<SessionType> pD = tell(d, 10000); 
            
            try {
                Session<SessionType> sD = pD.waitForSession();

                sD.sendIfAllowed("req", code);
                try{
                    Message mD = sD.waitForReceive(5 * 1000, "no", "ok");

                    switch (mD.getLabel()) {
                        case "no" :
                            sB.sendIfAllowed("unavailable");
                        case "ok" :
                            sB.sendIfAllowed("price", getPrice(code));
                    }
                } catch(TimeExpiredException e){
                    //the distributor did not respect its contract
                    sB.sendIfAllowed("unavailable");
                    
                    parallel(() -> {
                    	 sD.waitForReceive("no", "ok");
                    });
                }
            } 
            catch (ContractExpiredException e) {
            	sB.sendIfAllowed("unavailable");
            }
        }

    }

    private boolean isAvailable(String code){
        return code.equals("cavallo");
    }

    private String getPrice(String code){
        return code;
    }
    
    public static void main(String[] args) {
    	HonestyChecker.isHonest(Store.class);
    }
}
