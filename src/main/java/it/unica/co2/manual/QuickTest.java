package it.unica.co2.manual;

import static it.unica.co2.api.contract.utils.ContractFactory.def;
import static it.unica.co2.api.contract.utils.ContractFactory.internalSum;
import static it.unica.co2.api.contract.utils.ContractFactory.ref;

import co2api.ContractException;
import co2api.SessionI;
import it.unica.co2.api.contract.ContractDefinition;
import it.unica.co2.api.contract.SessionType;
import it.unica.co2.api.process.Participant;
import it.unica.co2.honesty.HonestyChecker;

public class QuickTest extends Participant {

	private static final long serialVersionUID = 1L;
	
	public QuickTest() {
		super("nicola@co2.unica.it", "nicola");
	}

	public static void main(String[] args) throws ContractException {
		HonestyChecker.isHonest(QuickTest.class);
//		new QuickTest().run();
	}
	
	@Override
	public void run() {
		
			ContractDefinition c = def("c");
			
			c.setContract(
					internalSum()
					.add("a", ref(c))
					.add("b")
				);
			
			SessionI<SessionType> x = tellAndWait(c);
			
			recur(x);
	}
	
	private void recur(SessionI<SessionType> x) {
		
		int i=1;
		
		if (1==i) {
			x.sendIfAllowed("a");
			recur(x);
		}
		else {
			x.sendIfAllowed("b");
		}
		
	}
}
