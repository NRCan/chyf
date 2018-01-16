package net.refractions.chyf.hygraph;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.refractions.chyf.enumTypes.FlowpathRank;
import net.refractions.chyf.enumTypes.FlowpathType;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

public class HyGraphBuilder {
	static final Logger logger = LoggerFactory.getLogger(HyGraphBuilder.class.getCanonicalName());

	private int nextEdgeId = 1;
	private int nextCatchmentId = 1;
	private List<Nexus> nexuses;
	private List<EFlowpath> eFlowpaths;
	private List<ECatchment> eCatchments;
	private Quadtree nexusIndex;
	private Quadtree eCatchmentIndex;

	public HyGraphBuilder() {
		this(1000);
	}

	public HyGraphBuilder(int capacity) {
		nexuses = new ArrayList<Nexus>(capacity);
		eFlowpaths = new ArrayList<EFlowpath>(capacity);
		eCatchments = new ArrayList<ECatchment>(capacity);
		nexusIndex = new Quadtree();
		eCatchmentIndex = new Quadtree();
	}
	
	public HyGraph build() {
		return new HyGraph(nexuses.toArray(new Nexus[nexuses.size()]), 
				eFlowpaths.toArray(new EFlowpath[eFlowpaths.size()]),
				eCatchments.toArray(new ECatchment[eCatchments.size()]));
	}
	
	public EFlowpath addEFlowpath(FlowpathType type, FlowpathRank rank, int strahlerOrder, int hortonOrder, int hackOrder, LineString lineString) {
		return addEFlowpath(getNexus(lineString.getStartPoint()), getNexus(lineString.getEndPoint()), 
				lineString.getLength(), type, rank, strahlerOrder, hortonOrder, hackOrder, getECatchment(lineString, type), lineString);
	}

	private EFlowpath addEFlowpath(Nexus fromNexus, Nexus toNexus, double length, 
			FlowpathType type, FlowpathRank rank, int strahlerOrder, int hortonOrder, int hackOrder, 
			ECatchment catchment, LineString lineString) {
		EFlowpath eFlowpath = new EFlowpath(nextEdgeId++, fromNexus, toNexus, length, type, rank, 
				strahlerOrder, hortonOrder, hackOrder, catchment, lineString);
		eFlowpaths.add(eFlowpath);
		fromNexus.addDownFlow(eFlowpath);
		toNexus.addUpFlow(eFlowpath);
		if(catchment != null) {
			catchment.addFlowpath(eFlowpath);
		} else {
			logger.warn("EFlowpath " + eFlowpath.getId() + " is not contained by any catchment.");
		}
		return eFlowpath;
	}

	public ECatchment addECatchment(Polygon polygon) {
		ECatchment eCatchment = new ECatchment(nextCatchmentId++, polygon); 
		eCatchments.add(eCatchment);
		eCatchmentIndex.insert(eCatchment.getEnvelope(), eCatchment);
		return eCatchment;
	}

	private ECatchment getECatchment(LineString lineString, FlowpathType type) {
		@SuppressWarnings("unchecked")
		List<ECatchment> possibleCatchments = eCatchmentIndex.query(lineString.getEnvelopeInternal());
		for(ECatchment catchment : possibleCatchments) {
			if(catchment.getPolygon().contains(lineString)) {
				return catchment;
			}
		}
		// fallback for if the flowpath is not contained by any ECatchment, but it is a bank flowpath
		// then just find the catchment containining the downstream point
		// as bank flowpaths may cross other catchments
		if(FlowpathType.BANK_FLOWPATH == type) {
			Point p = lineString.getEndPoint();
			for(ECatchment catchment : possibleCatchments) {
				if(catchment.getPolygon().contains(p)) {
					return catchment;
				}
			}			
		}
		return null;
	}
	
	private Nexus getNexus(Point point) {
		@SuppressWarnings("unchecked")
		List<Nexus> possibleNodes = nexusIndex.query(point.getEnvelopeInternal());
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
		nexusIndex.insert(node.getPoint().getEnvelopeInternal(),node);
		return node;
	}

}
