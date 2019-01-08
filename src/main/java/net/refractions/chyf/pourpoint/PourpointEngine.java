package net.refractions.chyf.pourpoint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jts.operation.union.UnaryUnionOp;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.enumTypes.CatchmentType;
import net.refractions.chyf.enumTypes.FlowpathType;
import net.refractions.chyf.enumTypes.NexusType;
import net.refractions.chyf.enumTypes.Rank;
import net.refractions.chyf.hygraph.DrainageArea;
import net.refractions.chyf.hygraph.ECatchment;
import net.refractions.chyf.hygraph.EFlowpath;
import net.refractions.chyf.hygraph.HyGraph;
import net.refractions.chyf.hygraph.Nexus;

/**
 * Computes the various pourpoint features.  See OutputType enum for
 * various options
 * 
 * @author Emily
 *
 */
public class PourpointEngine {

	static final Logger logger = LoggerFactory.getLogger(PourpointEngine.class.getCanonicalName());
	
	public enum OutputType{
		OUTPUT_PP("op", "Output Pourpoints"),	
		DISTANCE_MIN("opdmin", "Pourpoint Minimum Distance Matrix"), 
		DISTANCE_MAX("opdmax", "Pourpoint Maximum Distance Matrix"), 
		CATCHMENTS("pc", "Catchments"), 
		CATCHMENT_CONTAINMENT("pcc", "Catchment Containment Relationship"), 
		NONOVERLAPPING_CATCHMENTS("noc", "Non-Overlapping Catchments"), 
		NONOVERLAPPINGCATCHMENT_RELATIONSHIP("nocr", "Non-Overlapping Catchment Flow Relationships"), 
		TRAVERSAL_COMPLIANT_CATCHMENTS("tcc", "Transversal Compliant Catchments"), 
		TRAVERSAL_COMPLIANT_CATCHMENT_RELATION("tccr", "Transversal Compliant Catchment Flow Relationships"),
		INTERIOR_CATCHMENT("ic", "Interior Catchments");
		
		public String key;
		public String layername;
		
		OutputType(String key, String layername){
			this.key = key;
			this.layername = layername;
		}
		
		public static OutputType parse(String key) {
			for (OutputType t : OutputType.values()) {
				if (t.key.equalsIgnoreCase(key)) return t;
			}
			throw new IllegalStateException("Output type " + key + " not supported for pourpoint service");
		}
		
	}
	
	private List<Pourpoint> points;
	private HyGraph hygraph;
	private boolean removeHoles = false;
	
	private HashMap<PourpointKey, Range> distanceValues = new HashMap<>();
	private Set<OutputType> availableOutputs;
	private Set<PourpointKey> catchmentContainment;
	private Set<ECatchment> holes;
	
	public PourpointEngine(List<Pourpoint> points, HyGraph hygraph) {
		this.points = Collections.unmodifiableList(points);
		this.hygraph = hygraph;
	}
	
	public PourpointEngine(List<Pourpoint> points, HyGraph hygraph, boolean removeHoles) {
		this.points = Collections.unmodifiableList(points);
		this.hygraph = hygraph;
		this.removeHoles = removeHoles;
	}
	
	public Set<OutputType> getAvailableOutputs(){
		return this.availableOutputs;
	}
	
	public boolean getRemoveHoles() {
		return this.removeHoles;
	}
	
	/**
	 * Interior catchments merged together 
	 * @return
	 */
	public Set<DrainageArea> getInteriorCatchments(){
		if (holes == null || holes.isEmpty()) return Collections.emptySet();
		Geometry g = UnaryUnionOp.union(holes.stream().map(e->e.getPolygon()).collect(Collectors.toList()));
		if (g instanceof MultiPolygon) {
			Set<DrainageArea> dholes = new HashSet<>();
			for (int i = 0; i < ((MultiPolygon)g).getNumGeometries(); i ++) {
				dholes.add( new DrainageArea( ((MultiPolygon)g).getGeometryN(i) ));
			}
			return dholes;
		}else {
			return Collections.singleton(new DrainageArea(g));
		}
	}
	
	private boolean containsOutput(OutputType... types) {
		for (OutputType t : types) {
			if (availableOutputs.contains(t)) return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @param availableOutputs if null or empty everything is returned
	 * @return
	 */
	public PourpointOutput compute(Set<OutputType> availableOutputs) {
		
		if (availableOutputs == null) availableOutputs = new HashSet<>();
		if (availableOutputs.isEmpty()) {
			for (OutputType t : OutputType.values()) availableOutputs.add(t);
		}
		this.availableOutputs = availableOutputs;
			
		//compute downstream flowpaths for pourpoints
		points.forEach(p->p.findDownstreamFlowpaths(hygraph));
			
		//if two points have the same downstream flowpath then fail - this
		//is not allowed
		Set<EFlowpath> paths = new HashSet<>();
		for (Pourpoint p : points) {
			for (EFlowpath path : p.getDownstreamFlowpaths()) {
				if (paths.contains(path)) {
					//fail
					throw new PourpointException("Pourpoints must be unique.  You have multiple pourpoints that are projected to the same location.");
				}
				paths.add(path);
			}
		}
		
		//compute pourpoint relationship & distances between points
		if (containsOutput(OutputType.NONOVERLAPPINGCATCHMENT_RELATIONSHIP,
				OutputType.NONOVERLAPPING_CATCHMENTS,
				OutputType.TRAVERSAL_COMPLIANT_CATCHMENTS,
				OutputType.TRAVERSAL_COMPLIANT_CATCHMENT_RELATION,
				OutputType.DISTANCE_MAX,
				OutputType.DISTANCE_MIN,
				OutputType.INTERIOR_CATCHMENT)) {
			computeUpstreamDownstreamPourpointRelationship();
		}
		
		//catchments for pourpoints
		if (containsOutput(OutputType.CATCHMENTS,
				OutputType.NONOVERLAPPING_CATCHMENTS,
				OutputType.TRAVERSAL_COMPLIANT_CATCHMENTS,
				OutputType.TRAVERSAL_COMPLIANT_CATCHMENT_RELATION,
				OutputType.INTERIOR_CATCHMENT)) {
			computeUniqueCatchments();
		}
			
			
		if (availableOutputs.contains(OutputType.CATCHMENT_CONTAINMENT) || removeHoles) {
			computeCatchmentContainsRelationship();
		}
			
		//catchment relationships
		if (availableOutputs.contains(OutputType.TRAVERSAL_COMPLIANT_CATCHMENT_RELATION)) {
			computeUniqueSubCatchmentRelationship();
		}
	
		if (containsOutput(OutputType.INTERIOR_CATCHMENT) || 
			 (removeHoles && containsOutput(OutputType.CATCHMENTS, OutputType.NONOVERLAPPING_CATCHMENTS, OutputType.TRAVERSAL_COMPLIANT_CATCHMENTS))) {
			processHoles();
		}
		return new PourpointOutput(this);
	}
	
	
	public List<Pourpoint> getPoints(){
		return this.points;
	}
	
	/**
	 * sorted by pourpoint id
	 * @return
	 */
	public Integer[][] getCatchmentContainment(){
		if (!availableOutputs.contains(OutputType.CATCHMENT_CONTAINMENT) ) return null;

		ArrayList<Pourpoint> sorted = new ArrayList<>(points);
		sorted.sort((a,b)->a.getId().compareTo(b.getId()));
		
		Integer[][] results = new Integer[sorted.size()][sorted.size()];
		for (int i = 0; i < results.length; i ++) {
			for (int j = 0; j < results.length; j ++) {
				PourpointKey key1 = new PourpointKey(sorted.get(i),  sorted.get(j));
				PourpointKey key2 = new PourpointKey(sorted.get(j),  sorted.get(i));
				if (catchmentContainment.contains(key1)) {
					results[i][j] = 1;
				}else if (catchmentContainment.contains(key2) ) {
					results[i][j] = -1;
				}else {
					results[i][j] = null;
				}
			}
		}
		return results;
	}
	
	
	public Double[][] getProjectedPourpointMinDistanceMatrix(){
		if (!availableOutputs.contains(OutputType.DISTANCE_MIN) ) return null;
		return getPourpointDistanceMatrix(true);
	}
	public Double[][] getProjectedPourpointMaxDistanceMatrix(){
		if (!availableOutputs.contains(OutputType.DISTANCE_MAX) ) return null;
		return getPourpointDistanceMatrix(false);
	}
	
	private Double[][] getPourpointDistanceMatrix(boolean isMin){
		
		Double[][] values = new Double[points.size()][points.size()];
		
		for (int i = 0 ; i < points.size(); i ++) {
			for (int j = 0 ; j < points.size(); j ++) {
				if (i == j) { values[i][j] = null; continue; }
				
				Pourpoint pi = points.get(i);
				Pourpoint pj = points.get(j);
				
				Range range = distanceValues.get(new PourpointKey(pi, pj));
				int offset = -1;
				if (range == null) {
					range = distanceValues.get(new PourpointKey(pj, pi));
					offset = 1;
				}
				if (range == null) {
					values[i][j] = null;	
				}else {
					if (isMin) {
						values[i][j] = range.minDistance * offset;
					}else {
						values[i][j] = range.maxDistance *offset;
					}
				}
			}
		}
		return values;
	}
	
	
	public HashMap<PourpointKey, Range> getPourpointDistances(){
		return this.distanceValues;
	}
	

	
	/**
	 * results are ordered in the same order the pourpoints are 
	 * provided to the engine (see getPoints)
	 * @return
	 */
	public Integer[][] getNonOverlappingCoverageRelationship(){
		if (!availableOutputs.contains(OutputType.NONOVERLAPPINGCATCHMENT_RELATIONSHIP)) return null;
		Integer[][] values = new Integer[points.size()][points.size()];
	
		for (int i = 0 ; i < points.size(); i ++) {
			for (int j = 0 ; j < points.size(); j ++) {
				if (i == j) { values[i][j] = null; continue; }
				
				Pourpoint pi = points.get(i);
				Pourpoint pj = points.get(j);
				
				if (pi.getUpstreamPourpoints().contains(pj)) { values[i][j] = -1; continue;}
				if (pi.getDownstreamPourpoints().contains(pj)) { values[i][j] = 1; continue;}
				values[i][j] = null;
			}
		}
		return values;
	}
	
	public List<UniqueSubCatchment> getSortedTraveralCompliantCoverages(){
		if (!availableOutputs.contains(OutputType.TRAVERSAL_COMPLIANT_CATCHMENTS) && !availableOutputs.contains(OutputType.TRAVERSAL_COMPLIANT_CATCHMENT_RELATION)) return null;
		Set<UniqueSubCatchment> allCatchments = new HashSet<>();
		for (Pourpoint p : points) {
			allCatchments.addAll(p.getTraversalCompliantCatchments());
		}
		
		List<UniqueSubCatchment> ordered = new ArrayList<>();
		ordered.addAll(allCatchments);
		ordered.sort((a, b) -> a.getId().compareTo(b.getId()));
		return ordered;
}
	
	/**
	 * results are ordered by catchment id (see getSortedUniqueSubCatchments)
	 * @return
	 */
	public Integer[][] getTraversalCompliantCoverageRelationship(){
		if (!availableOutputs.contains(OutputType.TRAVERSAL_COMPLIANT_CATCHMENT_RELATION)) return null;
		List<UniqueSubCatchment> ordered = getSortedTraveralCompliantCoverages();
		Integer[][] values = new Integer[ordered.size()][ordered.size()];
		for (int i = 0 ; i < ordered.size(); i ++) {
			for (int j = 0 ; j < ordered.size(); j ++) {
				if (i == j) { values[i][j] = null; continue; }
				
				UniqueSubCatchment pi = ordered.get(i);
				UniqueSubCatchment pj = ordered.get(j);
				if (pi.isUpstream(pj)) {
					values[i][j] = -1;
				}else if (pj.isUpstream(pi)) {
					values[i][j] = 1;
				}else {
					values[i][j] = null;
				}
			}
		}
		return values;
	}
	
	
	private void computeUniqueSubCatchmentRelationship() {
		HashMap<EFlowpath, Set<UniqueSubCatchment>> flowToCatchments = new HashMap<>();
		for(Pourpoint p : points) {
			for (UniqueSubCatchment cat : p.getTraversalCompliantCatchments()) {
				for (ECatchment ecat : cat.getCatchments()) {
					for (EFlowpath flow : ecat.getFlowpaths()) {
						if (flowToCatchments.containsKey(flow)) {
							flowToCatchments.get(flow).add(cat);
						}else {
							Set<UniqueSubCatchment> items = new HashSet<>();
							items.add(cat);
							flowToCatchments.put(flow, items);
						}
					}
				}
			}
		}
				
		//compute immediate downstream relationship
		for(Pourpoint p : points) {
			for (UniqueSubCatchment cat : p.getTraversalCompliantCatchments()) {
				Set<EFlowpath> edgesOfInterest = new HashSet<>();
				cat.getCatchments().forEach(c->edgesOfInterest.addAll(c.getFlowpaths()));
			
				List<Nexus> downstreamNodes = new ArrayList<>();
				
				for (EFlowpath fp : edgesOfInterest) {
					int downcnt = 0;
					for (EFlowpath down : fp.getToNode().getDownFlows()) {
						if (edgesOfInterest.contains(down)) downcnt++;
					}
					if (downcnt == 0) downstreamNodes.add(fp.getToNode());
				}
				
				for (Nexus down : downstreamNodes) {
					List<EFlowpath> out = down.getDownFlows();
					for (EFlowpath o : out) {
						//find the 
						if (flowToCatchments.containsKey(o)) {
							Set<UniqueSubCatchment> u = flowToCatchments.get(o);
							for (UniqueSubCatchment uc : u) cat.addDownstreamCatchment(uc);
						}
					}
				}				
			}
		}

		//push downstream, computing upstream catchment computations
		for(Pourpoint p : points) {
			for (UniqueSubCatchment cat : p.getTraversalCompliantCatchments()) {
				cat.computeUpstreamCatchments();
			}
		}
	}

	
	
	private void computeUpstreamDownstreamPourpointRelationship() {
		HashMap<Nexus, HashMap<Pourpoint, Range>> nodedistances = new HashMap<>();
		distanceValues = new HashMap<>();
		
		for (Pourpoint pp : points) {
			//walk downstream until we reach the network terminus
			for (EFlowpath edge : pp.getDownstreamFlowpaths()) {
				processPourpointRel(pp, edge, nodedistances);
			}
		}	
		
		//this pourpoint relationship is the pourpoint catchment relationship
		//not the relationship of pourpoints along the flow network
		for (HashMap<Pourpoint, Range> nodeRel : nodedistances.values()) {
			for (Pourpoint p : nodeRel.keySet()) {
				Range d = nodeRel.get(p);
				if (d.getMinDistance() == -1) {
					HashSet<Pourpoint> items = new HashSet<>(nodeRel.keySet());
					//also remove other -1 items as these have the same headwaters
					for (Entry<Pourpoint, Range> rr : nodeRel.entrySet()) {
						if (rr.getValue().getMinDistance() == -1) items.remove(rr.getKey());
					}
					p.getUpstreamPourpoints().addAll(items);
					items.forEach(pr->pr.getDownstreamPourpoints().add(p));
				}
			}
		}
		
		//the distances between the pourpoints is computed based on the flow
		//network and is not associated in any way with the catchment relationship
		for (HashMap<Pourpoint, Range> nodeRel : nodedistances.values()) {
			for (Pourpoint p : nodeRel.keySet()) {
				Range d = nodeRel.get(p);
				if (d.getMinDistance() == 0) {
					//for all other pourpoints at this node
					for (Pourpoint p2 : nodeRel.keySet()) {		
						if (p2 == p) continue;
						if (nodeRel.get(p2).getMinDistance() == -1) continue;

						PourpointKey key = new PourpointKey(p, p2);
						Range r = distanceValues.get(key);
						if (r == null) {
							r = new Range();
							distanceValues.put(key, r);
						}
						r.updateDistance( nodeRel.get(p2).getMinDistance(), nodeRel.get(p2).getMaxDistance());
						
					}
				}
			}
		}
	}
	
	private void processPourpointRel(Pourpoint point, EFlowpath path, HashMap<Nexus, HashMap<Pourpoint, Range>> nodedistances) {
		//TODO: stop once we've visited all pourpoints as there won't be any more relationships downstream
		//this stop would only be useful if all the pourpoints are on the same network, if they are on
		//different trees it might not be useful
		ArrayDeque<EFlowpath> toProcess = new ArrayDeque<>();
		toProcess.add(path);
		while(!toProcess.isEmpty()) {
			EFlowpath item = toProcess.removeFirst();
		
			Nexus inNode = item.getFromNode();
			Nexus outNode = item.getToNode();
			
			HashMap<Pourpoint, Range> inValue = nodedistances.get(inNode);
			if (inValue == null) {
				inValue = new HashMap<>();
				nodedistances.put(inNode, inValue);
			}
			HashMap<Pourpoint, Range> outValue = nodedistances.get(outNode);
			if (outValue == null) {
				outValue = new HashMap<>();
				nodedistances.put(outNode, outValue);
			}
			
			double distance = item.getLength();
			boolean continueDownstream = true;
			//compute distances for downstream node		
			if (!inValue.containsKey(point)) {
				inValue.put(point, new Range(-1, -1));
				outValue.put(point,new Range(0,0));
			}else {
				Range v = inValue.get(point);
				Range outRange = outValue.get(point);
				if (outRange == null) outRange = new Range();
				if (v.minDistance <= 0) {
					outRange.updateDistance(distance, distance);
				}else {
					boolean isChanged = outRange.updateDistance(v.minDistance + distance, v.maxDistance+distance);
					continueDownstream = isChanged;
					//if this doesn't make a change to the distances we don't need to continue down this path
				}
				outValue.put(point, outRange);
			}
			
			if (continueDownstream) {
				for (EFlowpath downstream: outNode.getDownFlows()) {
					toProcess.addLast(downstream);
				}
			}
		}	
	}
	
	
	/**
	 * Computes the unique catchments and unique subcatchments for each pourpoint
	 */
	private void computeUniqueCatchments() {
		ArrayDeque<Pourpoint> toProcess = new ArrayDeque<>();
		
		for (Pourpoint pp : points) {
			if (pp.getUpstreamPourpoints().isEmpty()) toProcess.add(pp);
		}
		
		//tracks the secondary flow to post-process
		List<EFlowpath> secondaryPostProcess = new ArrayList<>(); 
		
		HashMap<EFlowpath, Set<ECatchment>> catchments = new HashMap<>();
		Set<Pourpoint> processed = new HashSet<>();
		while(!toProcess.isEmpty()) {
			Pourpoint item = toProcess.removeFirst();
			processed.add(item);
			for (EFlowpath path : item.getDownstreamFlowpaths()) {
				List<ECatchment>[] results = findUpstreamCatchments(item, path, catchments, secondaryPostProcess);
					
				List<ECatchment> uniqueCatchments = results[0];
				List<ECatchment> otherCatchments = results[1];
					
				item.addUpstreamCatchments(uniqueCatchments, otherCatchments);
				
				Set<ECatchment> all = new HashSet<>();
				all.addAll(uniqueCatchments);
				all.addAll(otherCatchments);
				catchments.put(path, all);
			}
			//remove pourpoint and find the next to process
			for (Pourpoint pp : points) {
				if (processed.contains(pp)) continue;
				if (toProcess.contains(pp)) continue;
				Set<Pourpoint> tt = new HashSet<>(pp.getUpstreamPourpoints());
				tt.removeAll(processed);
				if (tt.isEmpty()) toProcess.add(pp);
			}
		}
		
		//SECONDARY FLOW CATCHMENTS
		//post process secondary flows; if e catchment
		//is also in another catchment then walk upstream and remove
		//this from the nonoverlapping and traversal compliant catchments
		//note that the post processing order will be from the most upstream
		//to the most downstream pourpoints
		for (EFlowpath path : secondaryPostProcess) {		
			//find the primary flow catchments that the source
			//nexus flows into
			EFlowpath primary = null;
			for (EFlowpath outflow: path.getFromNode().getDownFlows()) {
				if (outflow.getRank() == Rank.PRIMARY) {
					primary = outflow;
					break;
				}
			}
			if (primary == null) continue; //TODO: walk upstream until we find out primary node?
			
			Pourpoint primaryPourpoint = null;
			for (Pourpoint p : points) {
				if (p.getUniqueCatchments().contains(primary.getCatchment())) {
					if (primaryPourpoint == null) {
						primaryPourpoint = p;
					}else if (primaryPourpoint.getUpstreamPourpoints().contains(p)){
						primaryPourpoint = p;
					}
				}
			}
			if (primaryPourpoint == null) continue;
			
			Set<Pourpoint> removeFrom = new HashSet<>();
			for (EFlowpath inflow : path.getFromNode().getUpFlows()) {
				ECatchment inCatchment = inflow.getCatchment();
				//we walk upstream removing this catchment from all pourpoints
				//except the primary one
				
				removeFrom.clear();
				for (Pourpoint p : points) {
					if (p.getUniqueCatchments().contains(inCatchment) && p != primaryPourpoint) {
						removeFrom.add(p);
					}
				}
				
				if (removeFrom.isEmpty()) continue;
				//walk upstream removing this catchments
				ArrayDeque<EFlowpath> up = new ArrayDeque<>();
				Set<EFlowpath> visited = new HashSet<>();
				up.add(inflow);
				while(!up.isEmpty()) {
					EFlowpath p = up.removeFirst();
					if (visited.contains(p)) continue;
					visited.add(p);
					if (p.getType() == FlowpathType.BANK) {
						removeFrom.forEach(cat -> cat.moveToSecondaryCatchment(p.getFromNode().getBankCatchment()));
						primaryPourpoint.getUniqueCatchments().add(p.getFromNode().getBankCatchment());
					}else {
						removeFrom.forEach(cat -> cat.moveToSecondaryCatchment(p.getCatchment()));
						primaryPourpoint.getUniqueCatchments().add(p.getCatchment());
					}
					
					for (EFlowpath in : p.getFromNode().getUpFlows()) {
						if (!visited.contains(in)) up.addLast(in);
					}
				}
			}
		}
		
		
		points.forEach(p->createUniqueSubCatchments(p));
		
	}
	
	/*
	 * compute catchment containment relationship
	 */
	private void computeCatchmentContainsRelationship() {
		catchmentContainment = new HashSet<>();
		for (Pourpoint point1 : points) {
			for (Pourpoint point2 : points) {
				if (point1 == point2) continue;
				
				PourpointKey key;
				if (point2.getSharedCatchments().size() > point1.getSharedCatchments().size()) {
					//point2 contains point2
					key = new PourpointKey(point2, point1);
				}else {
					//point1 contains point2
					key = new PourpointKey(point1, point2);
				}
				if (catchmentContainment.contains(key)) continue;
				
				for (ECatchment c : point1.getSharedCatchments()) {
					if (point2.getSharedCatchments().contains(c) || point2.getUniqueCatchments().contains(c)) {
						catchmentContainment.add(key);
						break;
					}
				}
			}	
		}
	}
	
	/*
	 * Computes the unique subcatchments
	 */
	private void createUniqueSubCatchments(Pourpoint point) {
		HashMap<ECatchment, UniqueSubCatchment> catchmentMapping = new HashMap<>();
		
		for (ECatchment e : point.getUniqueCatchments()) {
			UniqueSubCatchment ppc = catchmentMapping.get(e);
			if (ppc == null) {
				ppc = new UniqueSubCatchment(point);
				ppc.addCatchment(e);
				catchmentMapping.put(e,ppc);
			}
			
			if (e.getType() == CatchmentType.BANK) {
				//this has no flows and should be merged with whatever catchment it flows into
				continue;
			}
			List<Pourpoint> inpoints = new ArrayList<>();
			for (Nexus upN : e.getUpNexuses()) {
				for (EFlowpath inFlow : upN.getUpFlows()) {
					//if one of these flowpaths is part of a different pourpoint then we draw the line here
					for (Pourpoint pp : points) {
						if (pp == point) continue;
						if (pp.getDownstreamFlowpaths().contains(inFlow)) {
							inpoints.add(pp);
						}
					}
				}
			}
			for (Nexus downN : e.getDownNexuses()) {
				for (EFlowpath out : downN.getUpFlows()) {
					for (Pourpoint pp : points) {
						if (pp == point) continue;
						if (pp.getDownstreamFlowpaths().contains(out)) {
							ppc.addDownstreamPourpoint(pp);
						}
					}
				}
			}
			
			for (Pourpoint pp : inpoints) ppc.addUpstreamPourpoint(pp);
			
			if (inpoints.isEmpty()) {
				//combine this catchment with the unique catchments that are directly upstream
				List<ECatchment> upCatchments = new ArrayList<>();
				upCatchments.add(e);
				for (Nexus upN : e.getUpNexuses()) {
					if (upN.getType() == NexusType.BANK) {
						if (point.getUniqueCatchments().contains(upN.getBankCatchment())) {
							upCatchments.add(upN.getBankCatchment());
						}
					}else {
						for (EFlowpath inFlow : upN.getUpFlows()) {
							if (point.getUniqueCatchments().contains(inFlow.getCatchment())) {
								upCatchments.add(inFlow.getCatchment());
							}
						}
					}
				}
				for (int i = 1; i < upCatchments.size();i++) {
					if (catchmentMapping.containsKey(upCatchments.get(i))) ppc.mergeCatchment(catchmentMapping.get(upCatchments.get(i)));
				}
				for (ECatchment ec : upCatchments) ppc.addCatchment(ec);
				
				for (ECatchment upc : ppc.getCatchments()) {
					catchmentMapping.put(upc, ppc);
				}
			}else {
				ppc.addCatchment(e);
			}
			
		}
		
		for (ECatchment e : point.getUniqueCatchments()) {
			if (e.getType() == CatchmentType.BANK) {
				ECatchment addToo = e.getDownNexuses().get(0).getDownFlows().get(0).getCatchment();
				UniqueSubCatchment usc = catchmentMapping.get(addToo);
				if (usc != null) {
					usc.addCatchment(e);
					catchmentMapping.put(e, usc);
				}
			}
		}
		
		List<ECatchment> tomerge = new ArrayList<>(point.getDownstreamFlowpaths().size());
		for (EFlowpath p : point.getDownstreamFlowpaths()) {
			if (p.getType() == FlowpathType.BANK) {
				tomerge.add(p.getFromNode().getBankCatchment());
			}else {
				tomerge.add(p.getCatchment());
			}
		}
		UniqueSubCatchment root = catchmentMapping.get(tomerge.get(0));
		for (int i = 1; i < tomerge.size(); i ++) {
			root.mergeCatchment(catchmentMapping.get(tomerge.get(i)));
		}
		for (ECatchment e : root.getCatchments()) catchmentMapping.put(e, root);
		
		
		Set<UniqueSubCatchment> items = new HashSet<>();
		items.addAll(catchmentMapping.values());
		point.setTraversalCompliantCatchments(items);
	}
	
	
	private List<ECatchment>[] findUpstreamCatchments(Pourpoint point, EFlowpath root, HashMap<EFlowpath, Set<ECatchment>> catchments, List<EFlowpath> secondaryPostProcess ){
		
		List<ECatchment> uniqueCatchments = new ArrayList<ECatchment>();
		List<ECatchment> otherCatchments = new ArrayList<ECatchment>();
		
		ArrayDeque<EFlowpath> toProcess = new ArrayDeque<>();
		toProcess.add(root);
		Set<EFlowpath> visited = new HashSet<>();
		while(!toProcess.isEmpty()) {
			EFlowpath item = toProcess.removeFirst();
			visited.add(item);
			if (catchments.containsKey(item)) {
				otherCatchments.addAll(catchments.get(item));
			}else {
				ECatchment c = item.getCatchment();
				if(item.getType() == FlowpathType.BANK) {
					c = item.getFromNode().getBankCatchment();
				}
				uniqueCatchments.add(c);
				if (item.getType() != FlowpathType.BANK && item.getRank() == Rank.SECONDARY) {
					//track secondary flows for post processing
					if (!secondaryPostProcess.contains(item)) secondaryPostProcess.add(item);
				}
				for(EFlowpath f: item.getFromNode().getUpFlows()) {
					if (!visited.contains(f)) toProcess.addLast(f);
				}
				
			}	
		}
		uniqueCatchments.removeAll(otherCatchments);
		return new List[] {uniqueCatchments, otherCatchments};
	}

	private void processHoles() {
		this.holes = new HashSet<>();
		
		//find all the ecatchments that represent holes
		Set<ECatchment> holes = new HashSet<>();
		Set<ECatchment> wbholes = new HashSet<>();
		
		Set<Geometry> geometries = new HashSet<>();
		
		for (Pourpoint point : points) {
			DrainageArea area = point.getCatchmentDrainageAreaWithoutHoles();
			Polygon p = ((Polygon)area.getGeometry());
			geometries.add(p);
		}
		
		Geometry g = UnaryUnionOp.union(geometries);
		if (g instanceof Polygon) {
			g = g.getFactory().createMultiPolygon(new Polygon[]{(Polygon)g});
		}
		for (int j = 0; j < ((MultiPolygon)g).getNumGeometries(); j ++) {
			Polygon p = (Polygon) g.getGeometryN(j);
			for (int i = 0; i < p.getNumInteriorRing(); i ++) {
				LineString ls = p.getInteriorRingN(i);
				Polygon poly = p.getFactory().createPolygon(ls.getCoordinates());

				//this hole should get added to the unique catchments of this pourpoint
				List<ECatchment> findCatchments = hygraph.findECatchments(poly);
				for (ECatchment e : findCatchments) {	
					
					//make sure this is not already included in a catchments - this
					//occurs with these two points (-73.26971, 45.48318) (-73.27059, 45.45044)
					//at this catchment (-73.25636, 45.43621)
					boolean ishole = true;
					for (Pourpoint test : points) {
						if (test.getUniqueCatchments().contains(e)
								|| test.getSharedCatchments().contains(e)) {
							ishole = false;
						}
					}
					if (!ishole) continue;
					this.holes.add(e);
					
					if (e.getType().isWaterbody()) {
						wbholes.add(e);
					}else {
						holes.add(e);
					}
				}
			}
		}
		
		if (!removeHoles) return;
		
		//if this catchment is wholly contained within another isolated catchment then merge these two and ignore 
		// the inner one - this is the case with lakes
		HashMap<ECatchment, Set< ECatchment>> holeMapping = new HashMap<>();
		
		//map waterbody catchments to their exterior catchment
		for (ECatchment wbhole : wbholes) {
			//these need to be merged with their surround catchment
			for (EFlowpath flow : wbhole.getFlowpaths()) {
				if (flow.getType() == FlowpathType.BANK) {
					ECatchment mergeWith = flow.getFromNode().getBankCatchment();
					Set<ECatchment> items = holeMapping.get(mergeWith);
					if (items == null) {
						items = new HashSet<>();
						holeMapping.put(mergeWith, items);
					}
					items.add(wbhole);
				}
			}
		}
			
		for (ECatchment hole : holes) {
			//find the closest non-isolated flowpath edge
			List<EFlowpath> items = new ArrayList<>();
			Envelope env = hole.getEnvelope();
			items = hygraph.findEFlowpaths(env, item-> item.getCatchment() != null && item.getCatchment().getHackOrder() != null && item.getCatchment().getHackOrder() < 1001);
			double d = Math.max(env.getWidth(), env.getHeight());
			int count = 0;
			while(items.isEmpty()) {
				env.expandBy(d);
				items = hygraph.findEFlowpaths(env, item-> item.getCatchment() != null && item.getCatchment().getHackOrder() != null && item.getCatchment().getHackOrder() < 1001);
				count ++;
				if (count > ChyfDatastore.MAX_RESULTS) break;
			}
			//find the nearest edge
			EFlowpath closest = null;
			double distance = Math.max(env.getWidth(), env.getHeight()) * 2;
			for (EFlowpath p : items) {
				double pathd = DistanceOp.distance(p.getLineString(), hole.getPolygon());
				if (pathd < distance) {
					distance = pathd;
					closest = p;
				}
			}
			if (closest == null) {
				//nothing found skip this hole
				continue;
			}
			//merge holes in shared catchments
			for (Pourpoint p : points) {
				if (p.getSharedCatchments().contains(closest.getCatchment()) || p.getSecondaryCatchments().contains(closest.getCatchment())) {
					p.getSharedCatchments().add(hole);
					if (holeMapping.containsKey(hole)) {
						p.getSharedCatchments().addAll(holeMapping.get(hole));
					}
				}
			}
			//merge holes in unique catchments
			for (Pourpoint p : points) {
				if (p.getUniqueCatchments().contains(closest.getCatchment())) {
					p.getUniqueCatchments().add(hole);
					
					if (holeMapping.containsKey(hole)) {
						p.getUniqueCatchments().addAll(holeMapping.get(hole));
					}
					for (UniqueSubCatchment sub : p.getTraversalCompliantCatchments()) {
						if (sub.getCatchments().contains(closest.getCatchment())){
							sub.getCatchments().add(hole);
							
							if (holeMapping.containsKey(hole)) {
								sub.getCatchments().addAll(holeMapping.get(hole));
							}
						}
					}	
					break;
				}
			}	
		}
	}

	
	class Range{
		double minDistance;
		double maxDistance;
		
		public Range(double min, double max) {
			this.minDistance = min;
			this.maxDistance = max;
		}
		public Range() {
			minDistance = Double.POSITIVE_INFINITY;
			maxDistance = Double.NEGATIVE_INFINITY;
		}
		
		public boolean updateDistance(double min, double max) {
			boolean changed = false;
			if (min < minDistance) {
				minDistance = min;
				changed = true;
			}
			if (max > maxDistance) {
				maxDistance = max;
				changed = true;
			}
			return changed;
		}
		
		public double getMinDistance() { return this.minDistance; }
		public double getMaxDistance() { return this.maxDistance; }
	}
	
	class PourpointKey{
		Pourpoint from;
		Pourpoint to;
		
		public PourpointKey(Pourpoint from, Pourpoint to) {
			this.from = from;
			this.to = to;
		}
		@Override
		public int hashCode() {
			return Objects.hash(from,to);
		}
		@Override
		public boolean equals(Object other) {
			if (other == null) return false;
			if (other.getClass() != getClass()) return false;
			return Objects.equals(from, ((PourpointKey)other).from) && Objects.equals(to, ((PourpointKey)other).to);
		}
	}
}
