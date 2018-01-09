package net.refractions.chyf.hydrograph;

import java.util.Arrays;
import java.util.List;

import net.refractions.chyf.indexing.Filter;
import net.refractions.chyf.indexing.RTree;

import com.vividsolutions.jts.geom.Point;

public class HyGraph {
	private Nexus[] nexuses;
	private EFlowpath[] eFlowpaths;
	private RTree<EFlowpath> eFlowpathIndex;

	public HyGraph(Nexus[] nexuses, EFlowpath[] eFlowpaths) {
		this.nexuses = nexuses;
		this.eFlowpaths = eFlowpaths;
		eFlowpathIndex = new RTree<EFlowpath>(Arrays.asList(eFlowpaths));
	}
	
	public Nexus getNode(int nexusIndex) {
		return nexuses[nexusIndex];
	}

	public EFlowpath getEFlowpath(int eflowpathIndex) {
		return eFlowpaths[eflowpathIndex];
	}
	
	public List<EFlowpath> findEdges(Point p, int nResults, Integer maxDistance) {
		return eFlowpathIndex.search(p, nResults, maxDistance);
	}

	public List<EFlowpath> findEdges(Point p, int nResults, Integer maxDistance, Filter<EFlowpath> f) {
		return eFlowpathIndex.search(p, nResults, maxDistance, f);
	}

}
