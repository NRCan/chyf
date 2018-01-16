package net.refractions.chyf.hygraph;

import java.util.Arrays;
import java.util.List;

import net.refractions.chyf.indexing.ECatchmentContainsPointFilter;
import net.refractions.chyf.indexing.Filter;
import net.refractions.chyf.indexing.RTree;

import com.vividsolutions.jts.geom.Point;

public class HyGraph {
	private Nexus[] nexuses;
	private EFlowpath[] eFlowpaths;
	private ECatchment[] eCatchments;
	private RTree<Nexus> nexusIndex;
	private RTree<EFlowpath> eFlowpathIndex;
	private RTree<ECatchment> eCatchmentIndex;

	public HyGraph(Nexus[] nexuses, EFlowpath[] eFlowpaths, ECatchment[] eCatchments) {
		this.nexuses = nexuses;
		this.eFlowpaths = eFlowpaths;
		this.eCatchments = eCatchments;
		nexusIndex = new RTree<Nexus>(Arrays.asList(nexuses));
		eFlowpathIndex = new RTree<EFlowpath>(Arrays.asList(eFlowpaths));
		eCatchmentIndex = new RTree<ECatchment>(Arrays.asList(eCatchments));
	}
	
	public Nexus getNexus(int nexusId) {
		return nexuses[nexusId-1];
	}

	public EFlowpath getEFlowpath(int eflowpathId) {
		return eFlowpaths[eflowpathId-1];
	}

	/*
	 * Returns the flowpath that the give point would flow into,
	 * based on the elementary catchment the point is in.
	 * 
	 * @param point the point to search from
	 * @return the EFlowpath the point would flow into or null if the point
	 * 		is not contained in any elementary catchment
	 */
	public EFlowpath getEFlowpath(Point point) {

		List<ECatchment> eCatchments = eCatchmentIndex.search(point, 1, 0, 
				new ECatchmentContainsPointFilter(point));
		EFlowpath flowpath = null;
		if(eCatchments.size() > 0) {
			List<EFlowpath> possibleFlowpaths = eCatchments.get(0).getFlowpaths();
			switch(possibleFlowpaths.size()) {
				case 0:
					// must be a bank catchment, find the associated bank flowpath
					// TODO
					break;
				case 1:
					flowpath = possibleFlowpaths.get(0);
					break;
				default:
					// the catchment has multiple flowpaths, find the closest flowpath
					double dist = Double.POSITIVE_INFINITY;
					for(EFlowpath f: possibleFlowpaths) {
						if(f.distance(point) < dist) {
							flowpath = f;
						}
					}
			}
		}
		return flowpath;
	}
	
	public List<EFlowpath> findEFlowpaths(Point p, int nResults, Integer maxDistance, Filter<EFlowpath> f) {
		return eFlowpathIndex.search(p, nResults, maxDistance, f);
	}

	public ECatchment getECatchment(int id) {
		return eCatchments[id-1];
	}

	public List<ECatchment> findECatchments(Point p, int nResults, Integer maxDistance, Filter<ECatchment> f) {
		return eCatchmentIndex.search(p, nResults, maxDistance, f);
	}

	public List<Nexus> findNexuses(Point p, int nResults, Integer maxDistance, Filter<Nexus> f) {
		return nexusIndex.search(p, nResults, maxDistance, f);
	}

}
