package net.refractions.chyf.hygraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.refractions.chyf.indexing.ECatchmentContainsPointFilter;
import net.refractions.chyf.indexing.Filter;
import net.refractions.chyf.indexing.RTree;
import net.refractions.chyf.indexing.SpatiallyIndexable;

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

		List<ECatchment> eCatchments = eCatchmentIndex.search(point, 1, null, 
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
						double newDist = f.distance(point); 
						if(newDist < dist) {
							flowpath = f;
							dist = newDist; 
						}
					}
			}
		}
		return flowpath;
	}
	
	public List<EFlowpath> findEFlowpaths(Point p, int maxResults, Integer maxDistance, Filter<EFlowpath> f) {
		return eFlowpathIndex.search(p, maxResults, maxDistance, f);
	}

	public ECatchment getECatchment(int id) {
		return eCatchments[id-1];
	}

	public List<ECatchment> findECatchments(Point p, int maxResults, Integer maxDistance, Filter<ECatchment> f) {
		return eCatchmentIndex.search(p, maxResults, maxDistance, f);
	}

	public List<Nexus> findNexuses(Point p, int maxResults, Integer maxDistance, Filter<Nexus> f) {
		return nexusIndex.search(p, maxResults, maxDistance, f);
	}

	public List<SpatiallyIndexable> getECatchmentIndexNode(int id) {
		return eCatchmentIndex.getNode(id);
	}

	public List<EFlowpath> getUpstreamEFlowpaths(Point point, int maxResults) {
		EFlowpath eFlowpath = getEFlowpath(point);
		if(eFlowpath == null) {
			return Collections.emptyList();
		}
		List<EFlowpath> results = new ArrayList<EFlowpath>(maxResults);
		results.add(eFlowpath);
		for(int i = 0; i < results.size(); i++) {
			for(EFlowpath upstream: results.get(i).getFromNode().getUpFlows()) {
				results.add(upstream);
				if(results.size() >= maxResults) {
					break;
				}
			}
			if(results.size() >= maxResults) {
				break;
			}
		}
		return results;
	}

}
