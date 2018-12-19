package net.refractions.chyf.pourpoint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.refractions.chyf.enumTypes.CatchmentType;
import net.refractions.chyf.enumTypes.FlowpathType;
import net.refractions.chyf.enumTypes.NexusType;
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
		PROJECTED("ppp"),	//pp projected to hydro nexus
		DISTANCE_MIN("pppdmin"), //min distance along flowpath between projected pp
		DISTANCE_MAX("pppdmax"),  //max distance along flowpath between projected pp
		CATCHMENTS("pc"), //upstream catchments for pp
		NONOVERLAPPING_CATCHMENTS("noc"), //unique upstream catchments for pp
		NONOVERLAPPINGCATCHMENT_RELATIONSHIP("nocr"), //upstream/downstream relationship between nonoverlapping catchments
		TRAVERSAL_COMPLIANT_CATCHMENTS("tcc"), //unique upstream subcatchments for pp
		TRAVERSAL_COMPLIANT_CATCHMENT_RELATION("tccr"); //upstream/downstream relationships between upstream subcatchments
		
		public String key;
		
		OutputType(String key){
			this.key = key;
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
	 * 
	 * @param availableOutputs if null or empty everything is returned
	 * @return
	 */
	public PourpointOutput compute(Set<OutputType> availableOutputs) {
		try {
			if (availableOutputs == null) availableOutputs = new HashSet<>();
			if (availableOutputs.isEmpty()) {
				for (OutputType t : OutputType.values()) availableOutputs.add(t);
			}
			this.availableOutputs = availableOutputs;
			
			//compute downstream flowpaths for pourpoints
			points.forEach(p->p.findDownstreamFlowpaths(hygraph));
			
			//compute pourpoint relationship & distances between points
			if (availableOutputs.contains(OutputType.NONOVERLAPPINGCATCHMENT_RELATIONSHIP) ||
					availableOutputs.contains(OutputType.NONOVERLAPPING_CATCHMENTS) ||
					availableOutputs.contains(OutputType.TRAVERSAL_COMPLIANT_CATCHMENTS) ||
					availableOutputs.contains(OutputType.TRAVERSAL_COMPLIANT_CATCHMENT_RELATION) ||
					availableOutputs.contains(OutputType.DISTANCE_MAX) || 
					availableOutputs.contains(OutputType.DISTANCE_MIN)) {
				computeUpstreamDownstreamPourpointRelationship();
			}
			
			//catchments for pourpoints
			if (availableOutputs.contains(OutputType.CATCHMENTS) ||
				availableOutputs.contains(OutputType.NONOVERLAPPING_CATCHMENTS) ||
				availableOutputs.contains(OutputType.TRAVERSAL_COMPLIANT_CATCHMENTS) ||
				availableOutputs.contains(OutputType.TRAVERSAL_COMPLIANT_CATCHMENT_RELATION)) {
				computeUniqueCatchments();
			}
			
			//catchment relationships
			if (availableOutputs.contains(OutputType.TRAVERSAL_COMPLIANT_CATCHMENT_RELATION)) {
				computeUniqueSubCatchmentRelationship();
			}
	
			return new PourpointOutput(this);
		}catch(Throwable t) {
			logger.error("Pourpoint computation error", t);
			throw new RuntimeException(t);
		}
	}
	
	
	public List<Pourpoint> getPoints(){
		return this.points;
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
				int offset = 1;
				if (range == null) {
					range = distanceValues.get(new PourpointKey(pj, pi));
					offset = -1;
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
		
		HashMap<EFlowpath, Set<ECatchment>> catchments = new HashMap<>();
		Set<Pourpoint> processed = new HashSet<>();
		while(!toProcess.isEmpty()) {
			Pourpoint item = toProcess.removeFirst();
			processed.add(item);
			for (EFlowpath path : item.getDownstreamFlowpaths()) {
				List<ECatchment>[] results = findUpstreamCatchments(path, catchments);
					
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
		
		points.forEach(p->createUniqueSubCatchments(p));
		
	}
	
	
	/*
	 * Computes the unique subcatchments
	 */
	private void createUniqueSubCatchments(Pourpoint point) {
		HashMap<ECatchment, UniqueSubCatchment> catchmentMapping = new HashMap<>();
		
		for (ECatchment e : point.getNonOverlappingCatchments()) {
			UniqueSubCatchment ppc = catchmentMapping.get(e);
			if (ppc == null) {
				ppc = new UniqueSubCatchment(point, removeHoles);
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
			
			for (Pourpoint pp : inpoints) ppc.addUpstreamPourpoint(pp);
			
			if (inpoints.isEmpty()) {
				//combine this catchment with the unique catchments that are directly upstream
				List<ECatchment> upCatchments = new ArrayList<>();
				upCatchments.add(e);
				for (Nexus upN : e.getUpNexuses()) {
					if (upN.getType() == NexusType.BANK) {
						if (point.getNonOverlappingCatchments().contains(upN.getBankCatchment())) {
							upCatchments.add(upN.getBankCatchment());
						}
					}else {
						for (EFlowpath inFlow : upN.getUpFlows()) {
							if (point.getNonOverlappingCatchments().contains(inFlow.getCatchment())) {
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
		
		for (ECatchment e : point.getNonOverlappingCatchments()) {
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
	
	private List<ECatchment>[] findUpstreamCatchments(EFlowpath root, HashMap<EFlowpath, Set<ECatchment>> catchments){
		
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
				if(item.getType() == FlowpathType.BANK) {
					ECatchment cc = item.getFromNode().getBankCatchment();
					uniqueCatchments.add(cc);
				}else {
					if(item.getCatchment() != null) {
						ECatchment cc = item.getCatchment();
						uniqueCatchments.add(cc);
						for(EFlowpath f: item.getFromNode().getUpFlows()) {
							if (!visited.contains(f)) toProcess.addLast(f);
						}
					}
				}
			}
		}
		return new List[] {uniqueCatchments, otherCatchments};
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
