package net.refractions.chyf.hygraph;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.refractions.chyf.enumTypes.FlowpathType;
import net.refractions.chyf.enumTypes.NexusType;
import net.refractions.chyf.enumTypes.Rank;
import net.refractions.util.StopWatch;

public class StreamOrderCalculator {
	static final Logger logger = LoggerFactory.getLogger(StreamOrderCalculator.class.getCanonicalName());

	public static void calcOrders(List<EFlowpath> eFlowpaths, List<Nexus> nexuses) {
		logger.info("Calculating Stream Orders");
		StopWatch sw = new StopWatch();
		sw.start();
		
		// reset all stream orders to null everywhere
		for(EFlowpath f : eFlowpaths) {
			f.setStrahlerOrder(null);
			f.setHortonOrder(null);
			f.setHackOrder(null);
		}
		
		// loop through nexuses and start from each terminal nexus
		for(Nexus n : nexuses) {
			if(n.getType().isTerminal()) {
				for(EFlowpath f : n.getUpFlows()) {
					calcStrahlerOrder(f);
					MainstemmedFlowpath mf = new MainstemmedFlowpath(f);
					mf.assignHortonOrder(null);
					if (n.getType() == NexusType.TERMINAL_ISOLATED) {
						mf.assignHackOrder(1001);
					}else {
						mf.assignHackOrder(1);
					}
				}
			}
		}  
		
		sw.stop();
		logger.info("Stream Orders calculated in " + sw.getElapsedTime() + "ms");
	}
	
	private static Integer calcStrahlerOrder(EFlowpath f) {
		// skip eFlowpaths that are already done, secondary flows, or bank flowpaths
		if(f.getStrahlerOrder() != null || f.getRank() != Rank.PRIMARY || f.getType() == FlowpathType.BANK) {
			return f.getStrahlerOrder();
		}
		
		Integer maxUpflow = null;
		Integer order = 1;
		for(EFlowpath u : f.getFromNode().getUpFlows()) {
			Integer upOrder = calcStrahlerOrder(u);
			if(upOrder != null) {
				if(maxUpflow == null || upOrder > maxUpflow) {
					maxUpflow = upOrder; 
				} else if(maxUpflow == upOrder) {
					order = maxUpflow + 1;
				}
			}
		}
		if(maxUpflow != null && maxUpflow > order) {
			order = maxUpflow;
		}
		f.setStrahlerOrder(order);
		return order;
	}

}


class MainstemmedFlowpath {
	final EFlowpath f;
	MainstemmedFlowpath mainstem = null;
	double length = 0;
	ArrayList<MainstemmedFlowpath> upflows; 
	
	MainstemmedFlowpath(EFlowpath f) {

		this.f = f;

		upflows = new ArrayList<MainstemmedFlowpath>(f.getFromNode().getUpFlows().size());
		for(EFlowpath u : f.getFromNode().getUpFlows()) {
			if(u.getRank() == Rank.PRIMARY) {		
				upflows.add(new MainstemmedFlowpath(u));
			}
		}
		
		assignMainstem();
		
		length = f.getLength();
		if(mainstem != null) {
			length += mainstem.length;
		}
	}
	
	void assignMainstem() {
		MainstemmedFlowpath sameNameBest = null;
		MainstemmedFlowpath someNameBest = null;
		MainstemmedFlowpath noNameBest = null;
		for(MainstemmedFlowpath u : upflows) {
			// banks and secondaries can't be mainstems
			if(u.f.getStrahlerOrder() == null) continue;
			if(u.f.getName() == null || u.f.getName().isEmpty() || u.f.getName().equals(" ")) {
				if(noNameBest == null || u.length > noNameBest.length) {
					noNameBest = u;
				}
			} else if(u.f.getName().equals(f.getName())) {
				if(sameNameBest == null || u.length > sameNameBest.length) {
					sameNameBest = u;
				}
			} else {
				if(someNameBest == null || u.length > someNameBest.length) {
					someNameBest = u;
				}
			}
		}
		if(sameNameBest != null) {
			mainstem = sameNameBest;
		} else if(someNameBest != null) {
			mainstem = someNameBest;
		} else {
			mainstem = noNameBest;
		}
	}

	void assignHortonOrder(Integer order) {
		// don't assign to secondaries or banks
		if(f.getStrahlerOrder() == null) return;
		if(order == null) {
			f.setHortonOrder(f.getStrahlerOrder());
		} else {
			f.setHortonOrder(order);
		}
		for(MainstemmedFlowpath u : upflows) {
			if(mainstem == u) {
				u.assignHortonOrder(f.getHortonOrder());
			} else {
				u.assignHortonOrder(null);
			}
		}
	}
	
	/**
	 * "Bottom up" hierarchy that allocates the number "1" to the river with its mouth at the sea
	 * @param order : The hack order (river's number must be 1)
	 * @return The hack order
	 */
	Integer assignHackOrder(Integer order) {
		
		// skip eFlowpaths that are already done, secondary flows, or bank flowpaths
		if(f.getHackOrder() != null || f.getRank() != Rank.PRIMARY || f.getType() == FlowpathType.BANK) {
			return f.getHackOrder();
		}
		
		// set the first element to the number 1
 		if(f.getToNode().getType().isTerminal()) {
 			//isolated edges get hack order of 1001
 			if (f.getToNode().getType() == NexusType.TERMINAL_ISOLATED) {
				f.setHackOrder(1001);
			}else {
				f.setHackOrder(1);
			}
		}
		
		for(MainstemmedFlowpath u: upflows) {
			if(mainstem == u) {
				u.f.setHackOrder(u.assignHackOrder(order));
			} else {			
				u.f.setHackOrder(u.assignHackOrder(order + 1));
			}
		}
		return order;
	}
	
}