package net.refractions.chyf.pourpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.enumTypes.CatchmentType;
import net.refractions.chyf.enumTypes.FlowpathType;
import net.refractions.chyf.enumTypes.NexusType;
import net.refractions.chyf.hygraph.DrainageArea;
import net.refractions.chyf.hygraph.ECatchment;
import net.refractions.chyf.hygraph.EFlowpath;
import net.refractions.chyf.hygraph.HyGraph;
import net.refractions.chyf.hygraph.Nexus;
import net.refractions.chyf.rest.GeotoolsGeometryReprojector;

public class Pourpoint {
	
	static final Logger logger = LoggerFactory.getLogger(Pourpoint.class.getCanonicalName());

	public enum CType{
		NEAREST_INCATCHMENT(-2),
		NEAREST_FLOWPATH(-1),
		
		NEAREST_NEXUS_ALL(0),
		NEAREST_NEXUS_SINGLE(1);
		
		public int ccode;
		
		CType(int ccode){
			this.ccode = ccode;
		}
	}
	
	private String id;
	private Point location;
	private Point raw;
	private int ccode;
	private CType type;
	
	//start edges for the pourpoint
	private List<EFlowpath> downstreamFlowpaths = null;
	
	//pouroint relationship
	private Set<Pourpoint> downstreamPoints = null;
	private Set<Pourpoint> upstreamPoints = null;
	
	//unique catchments combined with upstream neighbors until
	//pourpoint nexus
	private Collection<UniqueSubCatchment> uniqueMergedSubCatchments;

	//catchments only used by this pourpoint
	private Set<ECatchment> uniqueCatchments;
	//catchments upstream of a secondary flow that are also contained
	//by another poutpoint
	private Set<ECatchment> secondaryCatchments;
	//all other catchments
	private Set<ECatchment> sharedCatchments;
	
	
//	private DrainageArea cachedDrainageArea;
	
	/**
	 * 
	 * @param location the point is assumed to be in the hygraph working projection
	 * @param ccode
	 */
	public Pourpoint(Point location, int ccode, String id) {
		this.id = id;
		this.raw = location;
		this.location = GeotoolsGeometryReprojector.reproject(location, ChyfDatastore.BASE_SRS);
		this.ccode = ccode;
		this.downstreamPoints = new HashSet<>();
		this.upstreamPoints = new HashSet<>();
		if (ccode == -2) {
			type = CType.NEAREST_INCATCHMENT;
		}else if (ccode == -1) {
			type = CType.NEAREST_FLOWPATH;
		}else if (ccode == 0) {
			type = CType.NEAREST_NEXUS_ALL;
		}else if (ccode > 0) {
			type = CType.NEAREST_NEXUS_SINGLE;
		}else {
			throw new IllegalStateException("Invalid ccode value");
		}
		
		uniqueCatchments = new HashSet<>();
		sharedCatchments = new HashSet<>();
		secondaryCatchments = new HashSet<>();
	}
	
	public int getCcode() {
		return this.ccode;
	}
	public void setTraversalCompliantCatchments( Collection<UniqueSubCatchment> mergedCatchments) {
		this.uniqueMergedSubCatchments = mergedCatchments;
	}
	
	public Collection<UniqueSubCatchment> getTraversalCompliantCatchments(){
		return this.uniqueMergedSubCatchments;
	}
	
	public void moveToSecondaryCatchment(ECatchment catchment) {
		secondaryCatchments.add(catchment);
		this.uniqueCatchments.remove(catchment);
		this.sharedCatchments.remove(catchment);
	}
	public void addUpstreamCatchments(Collection<ECatchment> uniqueCatchments, Collection<ECatchment> sharedCatchments) {
		this.uniqueCatchments.addAll(uniqueCatchments);
		this.sharedCatchments.addAll(sharedCatchments);
	}
	
	public Set<ECatchment> getUniqueCatchments(){
		return this.uniqueCatchments;
	}
	
	public Set<ECatchment> getSharedCatchments(){
		return this.sharedCatchments;
	}
	
	public Set<ECatchment> getSecondaryCatchments(){
		return this.secondaryCatchments;
	}
	
	public String getId() {
		return id;
	}
	public Set<Pourpoint> getDownstreamPourpoints(){
		return this.downstreamPoints;
	}
	public Set<Pourpoint> getUpstreamPourpoints(){
		return this.upstreamPoints;
	}
	
	public Point getProjectedPoint() {
		return this.downstreamFlowpaths.get(0).getToNode().getPoint();
	}
	
	public Point getPoint() {
		return this.location;
	}
	
	public Point getRawPoint() {
		return this.raw;
	}
	
	public List<EFlowpath> getDownstreamFlowpaths(){
		return this.downstreamFlowpaths;
	}
	
	
	public DrainageArea getCatchmentDrainageAreaWithoutHoles() {
//		if (this.cachedDrainageArea == null) {
			Set<ECatchment> items = new HashSet<>();
			items.addAll(getSharedCatchments());
			items.addAll(getUniqueCatchments());
			items.addAll(secondaryCatchments);
//			cachedDrainageArea = HyGraph.buildDrainageArea(items, false);
			return HyGraph.buildDrainageArea(items, false);
//		}
//		return cachedDrainageArea;
	}
	
	public DrainageArea getCatchmentDrainageArea(boolean removeHoles) {
		Set<ECatchment> items = new HashSet<>();
		items.addAll(getSharedCatchments());
		items.addAll(getUniqueCatchments());
		items.addAll(secondaryCatchments);
//		cachedDrainageArea = HyGraph.buildDrainageArea(items, false);
		return HyGraph.buildDrainageArea(items, false);
//		if (this.cachedDrainageArea == null) {
//			getCatchmentDrainageAreaWithoutHoles();
//		}
//		if (removeHoles) {
//			DrainageArea da = new DrainageArea(HyGraph.removeHoles(this.cachedDrainageArea.getGeometry()));
//			return da;
//		}else {
//			return this.cachedDrainageArea;
//		}
	}
	/**
	 * Finds the most downstream flowpath edges
	 * based code on of the point
	 * 
	 * @param graph
	 */
	public void findDownstreamFlowpaths(HyGraph graph) {
		downstreamFlowpaths = new ArrayList<>();
		if (this.type == CType.NEAREST_INCATCHMENT) {
			//find the elementary catchment and 
			//then find the most downstream
			//edge in that catchment
			
			List<ECatchment> catchments = graph.findECatchments(location, 1, 0, null);
			if (catchments.isEmpty() || catchments.size() > 1) {
				logger.error("Pourpoint not located in any catchment (" + raw.getX() + ", " + raw.getY()+ ")");
				return;
			}
			ECatchment c = catchments.get(0);
			
			if (c.getType() == CatchmentType.BANK) {
				EFlowpath path = c.getDownNexuses().get(0).getDownFlows().get(0);
				//should be a bank type
				downstreamFlowpaths.add(path);
			}else {
				//find the flowpath "nearest" to 
				double distance = Double.MAX_VALUE;
				EFlowpath closest = null;
				for (EFlowpath p : c.getFlowpaths()) {
					double tdistance = p.getToNode().getPoint().getCoordinate().distance(location.getCoordinate());
					if (tdistance < distance) {
						closest = p;
						distance = tdistance;
					}
				}
				if (closest != null) {
					for (EFlowpath p : closest.getToNode().getUpFlows()) {
						if (p.getCatchment() == c) {
							downstreamFlowpaths.add(p);
						}
					}
				}
			}
		}else if (this.type == CType.NEAREST_FLOWPATH) {
			//The intended pourpoint is the nearest point on the nearest flowpath 
			//(using Euclidean distance in both cases) to the input pourpoint.
			//find the nearest flowpath
			List<EFlowpath> nearestPaths = graph.findEFlowpaths(location, 4, null, e->e.getType() != FlowpathType.BANK);
			
			if (nearestPaths.isEmpty()) {
				logger.error("Pourpoint not located near any flowpaths(" + raw.getX() + ", " + raw.getY()+ ")");
			}else {
				double d0 = nearestPaths.get(0).distance(location);
				double d1 = nearestPaths.get(1).distance(location);
				
				if(d0 < d1) {
					downstreamFlowpaths.add(nearestPaths.get(0));
				}else {
					//multiple points with same distance; add the one in the same catchments
					//find the first with the closest downstream path
					EFlowpath toAdd = nearestPaths.get(0);
					double dn0 = nearestPaths.get(0).getToNode().distance(location);
					for (int i = 1; i < nearestPaths.size(); i ++) {
						if (nearestPaths.get(i).distance(location) != d0) break;
						double dnx = nearestPaths.get(i).getToNode().distance(location);
						if (dnx < dn0) {
							dn0 = dnx;
							toAdd = nearestPaths.get(i);
						}
					}
					downstreamFlowpaths.add(toAdd);
				}
			}
			
		}else if (this.type == CType.NEAREST_NEXUS_ALL) {
			//the closest hydro nexus is the intended pourpoint, and 
			//the intended catchment includes all
			//areas that drain into that nexus. If two streams drain into that nexus, 
			//then the catchment of interest includes the catchments of both streams.
			
			List<Nexus> nearestNexus = graph.findNexuses(location, 1, null, e->e.getType() != NexusType.BANK);
			if (nearestNexus.isEmpty()) {
				logger.error("Pourpoint not located near any nexuses (" + raw.getX() + ", " + raw.getY()+ ")");
			}else {
				nearestNexus.get(0).getUpFlows()
					.stream()
					.filter(f->f.getType() != FlowpathType.BANK)
					.forEach(f->downstreamFlowpaths.add(f));				
			}
		}else if (this.type == CType.NEAREST_NEXUS_SINGLE) {
			//The closest hydro nexus is the intended pourpoint, and 
			//which of the inflowing catchments is of interest. 
			//1 means that the catchment of interest is the catchment for the first 
			//inflowing flowpath, 2 means that the intended catchment is the 
			//second inflowing flowpath, and so on. Values greater than 2 are possible but rare. 
			//The positive c-code value corresponds to the order in which 
			//the flowpath is encountered, when rotating around the nexus in a 
			//clockwise direction from the outflowing flowpath.
			List<Nexus> nearestNexus = graph.findNexuses(location, 1, null, e->e.getType() != NexusType.BANK);
			if (nearestNexus.isEmpty()) {
				logger.error("Pourpoint near any nexuses (" + raw.getX() + ", " + raw.getY()+ ")");
			}else {
				//first filter out bank flows
				List<EFlowpath> temp = new ArrayList<>();
				nearestNexus.get(0).getUpFlows()
					.stream()
					.filter(f->f.getType() != FlowpathType.BANK)
					.forEach(f->temp.add(f));
				
				if (this.ccode > temp.size()) {
					logger.error("Pourpoint projection - no non-back flowpaths found (" + raw.getX() + ", " + raw.getY()+ ")");
				}
				if (temp.size() == 1 && this.ccode == 1) {
					//only one to return so we are 
					downstreamFlowpaths.add(temp.get(0));
				}else {
					if (nearestNexus.get(0).getDownFlows().isEmpty()) {
						logger.error("Pourpoint projection - no down flows from nexus (" + raw.getX() + ", " + raw.getY()+ ")");
					}
					
					//sort outputs by type
					EFlowpath primaryOutput = nearestNexus.get(0).getDownFlows().get(0);
					if (nearestNexus.get(0).getDownFlows().size() > 1) {
						List<EFlowpath> sortedOuts = new ArrayList<>(nearestNexus.get(0).getDownFlows());
						sortedOuts.sort((a,b)->Integer.compare(a.getRank().ordinal(), b.getRank().ordinal()));
						primaryOutput = sortedOuts.get(0);
					}
					
					Coordinate cOutflow = primaryOutput.getLineString().getCoordinateN(1);
					Coordinate cNexus = nearestNexus.get(0).getPoint().getCoordinate();
					
					//sort angles on value
					SortedMap<Double,EFlowpath> angles = new TreeMap<Double, EFlowpath>();
					for (EFlowpath path : temp) {
						Coordinate cInflow = path.getLineString().getCoordinateN(1);
						double angle = Angle.angleBetweenOriented(cOutflow, cNexus, cInflow);
						if (angle > 0)  angle = angle - Angle.PI_TIMES_2;
						angles.put(-angle, path);
					}
				
					Iterator<EFlowpath> it = angles.values().iterator();
					EFlowpath item = it.next();
					int i = 1;
					while(i < ccode) { i++; item = it.next(); }
					downstreamFlowpaths.add(item);
					
				}
			}
		}
		
		if (downstreamFlowpaths.size() == 1) {
			EFlowpath down = downstreamFlowpaths.get(0);
			if (down.getToNode().getType() == NexusType.FLOWPATH 
					&& down.getToNode().getDownFlows().size() == 1 
					&& down.getToNode().getUpFlows().size() == 1 
					&& down.getToNode().getDownFlows().get(0).getCatchment() == down.getCatchment()) {
				//two degree nexus node on single line flow path
				//for this special case we walk down until we meet a catchment boundary
				while(down.getToNode().getType() == NexusType.FLOWPATH 
						&& down.getToNode().getDownFlows().size() == 1 
						&& down.getToNode().getUpFlows().size() == 1 
						&& down.getToNode().getDownFlows().get(0).getCatchment() == down.getCatchment()) {
					
					down = down.getToNode().getDownFlows().get(0);
				}
				downstreamFlowpaths.clear();
				downstreamFlowpaths.add(down);
			}
			
		}
		
	}
}
