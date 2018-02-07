package net.refractions.chyf.hygraph;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import net.refractions.chyf.enumTypes.CatchmentType;
import net.refractions.chyf.enumTypes.FlowpathType;
import net.refractions.chyf.enumTypes.NexusType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.IntersectionMatrix;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.index.quadtree.Quadtree;

public class HyGraphBuilder {
	static final Logger logger = LoggerFactory.getLogger(HyGraphBuilder.class.getCanonicalName());

	private int nextNexusId = 1;
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
		classifyNexuses();
		classifyCatchments();
		return new HyGraph(nexuses.toArray(new Nexus[nexuses.size()]), 
				eFlowpaths.toArray(new EFlowpath[eFlowpaths.size()]),
				eCatchments.toArray(new ECatchment[eCatchments.size()]));
	}
	
	public EFlowpath addEFlowpath(FlowpathType type, int rank, String name, int strahlerOrder, int hortonOrder, int hackOrder, LineString lineString) {
		return addEFlowpath(getNexus(lineString.getStartPoint()), getNexus(lineString.getEndPoint()), 
				lineString.getLength(), type, rank, name, strahlerOrder, hortonOrder, hackOrder, getECatchment(lineString, type), lineString);
	}

	private EFlowpath addEFlowpath(Nexus fromNexus, Nexus toNexus, double length, 
			FlowpathType type, int rank, String name, int strahlerOrder, int hortonOrder, int hackOrder, 
			ECatchment catchment, LineString lineString) {
		EFlowpath eFlowpath = new EFlowpath(nextEdgeId++, fromNexus, toNexus, length, type, rank, name, 
				strahlerOrder, hortonOrder, hackOrder, catchment, lineString);
		eFlowpaths.add(eFlowpath);
		fromNexus.addDownFlow(eFlowpath);
		toNexus.addUpFlow(eFlowpath);
		if(catchment != null) {
			catchment.addFlowpath(eFlowpath);
			if(catchment.getPolygon().touches(fromNexus.getPoint())) {
				catchment.addUpNexus(fromNexus);
			}
			if(catchment.getPolygon().touches(toNexus.getPoint())) {
				catchment.addDownNexus(toNexus);
			}
		} else {
			logger.warn("EFlowpath " + eFlowpath.getId() + " is not contained by any catchment.");
		}
		return eFlowpath;
	}

	public ECatchment addECatchment(CatchmentType type, Polygon polygon) {
		@SuppressWarnings("unchecked")
		List<ECatchment> possibleCatchments = eCatchmentIndex.query(polygon.getEnvelopeInternal());
		for(ECatchment catchment : possibleCatchments) {
			IntersectionMatrix matrix = catchment.getPolygon().relate(polygon);
			if(matrix.isEquals(3,3)) {
				// if the pre-existing matching catchment is not a WaterCatchment
				// or the newly added catchment is a WaterCatchment
				if(!catchment.getType().toString().equals("Water")
						|| type.toString().equals("Water")) {
					// something is wrong
					logger.warn("Identical catchments; catchment id: " + catchment.getId() );
				}
				// don't create the duplicate, return the original
				return catchment;
			} else if(matrix.matches("T********")) {
				logger.warn("Overlapping catchments; catchment id: " + catchment.getId() );
				return null;
			}
		}
		// not a duplicate so create and add it
		ECatchment eCatchment = new ECatchment(nextCatchmentId++, type, polygon);
		eCatchments.add(eCatchment);
		eCatchmentIndex.insert(eCatchment.getEnvelope(), eCatchment);
		return eCatchment;
	}

	private ECatchment getECatchment(LineString lineString, FlowpathType type) {
		@SuppressWarnings("unchecked")
		List<ECatchment> possibleCatchments = eCatchmentIndex.query(lineString.getEnvelopeInternal());
		ECatchment c = null;
		int count = 0;
		for(ECatchment catchment : possibleCatchments) {
			if(catchment.getPolygon().contains(lineString)) {
				c = catchment;
				count++;
			}
		}
		if(count > 1) {
			logger.warn("Flowpath is in multiple catchments, such as catchment " + c.getId());
		}
		if(c != null) {
			return c;
		}
		// fallback for if the flowpath is not contained by any ECatchment, but it is a bank flowpath
		// then just find the catchment containining the downstream point
		// as bank flowpaths may cross other catchments
		if(FlowpathType.BANK == type) {
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
		Nexus node = new Nexus(nextNexusId++, point);
		nexuses.add(node);
		nexusIndex.insert(node.getPoint().getEnvelopeInternal(),node);
		return node;
	}

	private void classifyNexuses() {
		for(Nexus n : nexuses) {
			if(n.getUpFlows().size() == 0) {
				if(n.getDownFlows().size() == 1 
						&& n.getDownFlows().get(0).getType() == FlowpathType.BANK) {
					n.setType(NexusType.BANK);
				} else { 
					n.setType(NexusType.HEADWATER);
				}
			} else if(n.getDownFlows().size() == 0) {
				n.setType(NexusType.TERMINAL);
			} else {
				// count up how many of each type of flowpath we have going each direction
				EnumMap<FlowpathType,Integer> upTypes = new EnumMap<FlowpathType,Integer>(FlowpathType.class);
				EnumMap<FlowpathType,Integer> downTypes = new EnumMap<FlowpathType,Integer>(FlowpathType.class);
				for(FlowpathType t : FlowpathType.values()) {
					upTypes.put(t, 0);
					downTypes.put(t, 0);
				}
				for(EFlowpath f : n.getUpFlows()) {
					upTypes.put(f.getType(), upTypes.get(f.getType())+1);
				}
				for(EFlowpath f : n.getDownFlows()) {
					downTypes.put(f.getType(), downTypes.get(f.getType())+1);
				}
				if(upTypes.get(FlowpathType.INFERRED) == 1 && n.getUpFlows().size() == 1 
						&& downTypes.get(FlowpathType.INFERRED) == 1 && n.getDownFlows().size() == 1) {
					// just two inferred
					n.setType(NexusType.WATER);
				} else if(upTypes.get(FlowpathType.INFERRED) + upTypes.get(FlowpathType.BANK) == n.getUpFlows().size()
						&& downTypes.get(FlowpathType.INFERRED) + downTypes.get(FlowpathType.BANK)== n.getDownFlows().size()) {
					// all inferred and bank
					n.setType(NexusType.INFERRED);
				} else {
					// TODO could check for other wierd/unexpected combinations of up/downflows
					// but for now we will assume this is a regular flowpath nexus
					n.setType(NexusType.FLOWPATH);
				}
			}
		}
	}
	
	private void classifyCatchments() {
		for(ECatchment c : eCatchments) {
			if(c.getFlowpaths().size() == 0) {
				@SuppressWarnings("unchecked")
				List<Nexus> possibleNexuses = nexusIndex.query(c.getEnvelope());
				Nexus bankNexus = null;
				int bankNexuses = 0;
				int totalNexuses = 0;
				for(Nexus n: possibleNexuses) {
					if(c.getPolygon().touches(n.getPoint())) {
						totalNexuses++;
						if(n.getType().equals(NexusType.BANK)) {
							bankNexuses++;
							bankNexus = n;
						}
					}
				}
				if(totalNexuses == 0) {
					c.setType(CatchmentType.EMPTY);
				} else if(bankNexuses == 1) {
					c.addDownNexus(bankNexus);
					bankNexus.setBankCatchment(c);
					c.setType(CatchmentType.BANK);
				} else {
					logger.warn("Catchment " + c.getId() + " has no flowpaths and an unexpected number of nexuses (" + totalNexuses + ").");
					c.setType(CatchmentType.UNKNOWN);
				}
			} else {
				if(c.getType() == null || c.getType() == CatchmentType.UNKNOWN) {
					// Water types will have already been assigned
					c.setType(CatchmentType.REACH);
				}
			}
		}
	}
}
