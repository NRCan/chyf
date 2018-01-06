package net.refractions.chyf.hydrograph;

import java.util.ArrayList;
import java.util.List;

import net.refractions.chyf.enumTypes.FlowpathRank;
import net.refractions.chyf.enumTypes.FlowpathType;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.quadtree.Quadtree;

public class HyGraphBuilder {
	private int nextEdgeId = 1;
	private List<Nexus> nexuses;
	private List<EFlowpath> eFlowpaths;
	private Quadtree nodeIndex;

	public HyGraphBuilder() {
		this(1000,1000);
	}

	public HyGraphBuilder(int nodeCapacity, int edgeCapacity) {
		nexuses = new ArrayList<Nexus>(nodeCapacity);
		eFlowpaths = new ArrayList<EFlowpath>(edgeCapacity);
		nodeIndex = new Quadtree();
	}
	
	public HyGraph build() {
		return new HyGraph(nexuses.toArray(new Nexus[nexuses.size()]), eFlowpaths.toArray(new EFlowpath[eFlowpaths.size()]));
	}
	
	public EFlowpath addEFlowpath(FlowpathType type, FlowpathRank rank, LineString lineString) {
		return addEFlowpath(getNexus(lineString.getStartPoint()), getNexus(lineString.getEndPoint()), lineString.getLength(), type, rank, lineString);
	}

	private EFlowpath addEFlowpath(Nexus fromNexus, Nexus toNexus, double length, FlowpathType type, FlowpathRank rank, LineString lineString) {
		EFlowpath eFlowpath = new EFlowpath(nextEdgeId++, fromNexus, toNexus, length, type, rank, lineString);
		eFlowpaths.add(eFlowpath);
		fromNexus.addDownFlow(eFlowpath);
		toNexus.addUpFlow(eFlowpath);
		return eFlowpath;
	}
	
	private Nexus getNexus(Point point) {
		@SuppressWarnings("unchecked")
		List<Nexus> possibleNodes = nodeIndex.query(point.getEnvelopeInternal());
		for(Nexus node : possibleNodes) {
			if(point.equals(node.getPoint())) {
				return node;
			}
		}
		return addNexus(point);
	}

	public Nexus addNexus(Point point) {
		Nexus node = new Nexus(point);
		nexuses.add(node);
		nodeIndex.insert(node.getPoint().getEnvelopeInternal(),node);
		return node;
	}

}
