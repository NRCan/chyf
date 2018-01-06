package net.refractions.chyf.hydrograph;

import java.util.Arrays;

import com.vividsolutions.jts.geom.Point;

import net.refractions.chyf.indexing.RTree;


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
	
	public EFlowpath nearestEdge(Point p) {
		return eFlowpathIndex.search(p, 1, null).get(0);
	}

}
