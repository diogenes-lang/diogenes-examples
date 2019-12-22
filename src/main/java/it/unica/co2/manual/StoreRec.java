package it.unica.co2.manual;

import java.util.ArrayList;
import java.util.List;

import co2api.CO2ServerConnection;
import co2api.ContractExpiredException;
import co2api.ContractViolationException;
import co2api.Message;
import co2api.Public;
import co2api.Session;
import co2api.TST;

public class StoreRec{

	public static void main(String[] args) {
		
		CO2ServerConnection co2 = new CO2ServerConnection("testuser@co2.unica.it", "pa55w0rd");
		
		new Thread(()->{
			// buyer
			String buyer = "REC 'x' [!addtocart{t<60;t}.'x' + !checkout{t<60;t}.(?price{t<20;t}.(!accept{t<10} + !reject{t<10}) & ?unavailable{t<20})]";
			
			TST c = new TST(buyer);
			Session<TST> s = c.toPrivate(co2).tell().waitForSession();

			s.send("addtocart", 0x00);
			s.send("addtocart", 0x01);
			s.send("addtocart", 0x02);
			
			s.send("checkout");
			
			Message msg = s.waitForReceive();
			
			if (msg.getLabel().equals("price")) {
				s.send("reject");		// accept or reject					
			}
			else if (msg.getLabel().equals("unavailable")) {
				// finished
			}
			else {
				throw new IllegalStateException();
			}
			
			
		}, "buyer").start();
		
		new Thread(()->{
			//distributor
			TST c = new TST("?req{;t}.(!ok{t<5} + !no{t<5})");
			
			Session<TST> s = c.toPrivate(co2).tell().waitForSession();
			
			Message msg = s.waitForReceive();
			
			if (msg.getLabel().equals("req"))
				s.send("no");	// ok or no
						
		}, "distributor").start();
		
		new Thread(()->{
			
			TST cB = new TST("REC 'x' [?addtocart{t<60;t}.'x' & ?checkout{t<60;t}.(!price{t<20;t}.(?accept{t<10} & ?reject{t<10})+ !unavailable{t<20})]");
			TST cD = new TST("!req{;t}.(?ok{t<5} & ?no{t<5})");

			Public<TST>  pB = cB.toPrivate(co2).tell();
			Session<TST> sB = pB.waitForSession();
			List<String> orders = new ArrayList<>();
			Message mB;

			try {
			    do {
			        mB = sB.waitForReceive();
			        if (mB.getLabel().equals("addtocart")){
			            orders.add(mB.getStringValue()); 
			        }
			    } while(!mB.getLabel().equals("checkout"));
			    
			    boolean condition = false;
			    
			    if (condition) { // handled internally
			        sB.send("price", 10);
			        String res = sB.waitForReceive().getLabel();
			        switch (res){
			            case "accept": // handle the order
			            case "reject": // terminate
			        }
			    } 
			    else { // handled with the distributor
			        Public<TST> pD = cD.toPrivate(co2).tell(5 * 1000);
			        try {
			            Session<TST> sD = pD.waitForSession();
			            sD.send("req", "1234");
			            try{
			                switch (sD.waitForReceive().getLabel()) {
			                case "no": 
			                    sB.send("unavailable"); 
			                    break;
			                case "ok":
			                    sB.send("price", 42);
			                    try{
			                        String res = 
			                          sB.waitForReceive().getLabel();
			                        switch (res) {
			                        case "accept": // handle the order
			                        case "reject": // terminate
			                        }
			                    }
			                    catch (ContractViolationException e) {
			                        //the buyer is culpable, terminate
			                    	e.printStackTrace();
			                    }
			                }
			            } catch(ContractViolationException e){
			                //the distributor did not respect its contract
			                sB.send("unavailable");
			                e.printStackTrace();
			            }
			        } 
			        catch (ContractExpiredException e) {
			            //no distributor found
			            sB.send("unavailable");
			            e.printStackTrace();
			        }
			    }
			} catch (ContractViolationException e){
				e.printStackTrace();
			    //the buyer is culpable, terminate
			}
		}, "store").start();
	}
}
