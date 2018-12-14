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

	public enum OutputType{
		PROJECTED("ppp"),	//pp projected to hydro nexus
		DISTANCE_MIN("pdmin"), //min distance along flowpath between projected pp
		DISTANCE_MAX("pdmax"),  //max distance along flowpath between projected pp
		PP_RELATIONSHIP("pr"), //upstream/downstream relationship between pp
		CATCHMENTS("pc"), //upstream catchments for pp
		UNIQUE_CATCHMENTS("puc"), //unique upstream catchments for pp
		UNIQUE_SUBCATCHMENTS("pusc"), //unique upstream subcatchments for pp
		UNIQUE_SUBCATCHMENTS_RELATION("puscr"); //upstream/downstream relationships between upstream subcatchments
		
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
		if (availableOutputs == null) availableOutputs = new HashSet<>();
		if (availableOutputs.isEmpty()) {
			for (OutputType t : OutputType.values()) availableOutputs.add(t);
		}
		this.availableOutputs = availableOutputs;
		
		//compute downstream flowpaths for pourpoints
		points.forEach(p->p.findDownstreamFlowpaths(hygraph));
		
		//compute pourpoint relationship & distances between points
		if (availableOutputs.contains(OutputType.PP_RELATIONSHIP) ||
				availableOutputs.contains(OutputType.UNIQUE_CATCHMENTS) ||
				availableOutputs.contains(OutputType.UNIQUE_SUBCATCHMENTS) ||
				availableOutputs.contains(OutputType.UNIQUE_SUBCATCHMENTS_RELATION)) {
			computeUpstreamDownstreamPourpointRelationship();
		}
		
		//catchments for pourpoints
		if (availableOutputs.contains(OutputType.CATCHMENTS) ||
			availableOutputs.contains(OutputType.UNIQUE_CATCHMENTS) ||
			availableOutputs.contains(OutputType.UNIQUE_SUBCATCHMENTS) ||
			availableOutputs.contains(OutputType.UNIQUE_SUBCATCHMENTS_RELATION)) {
			computeUniqueCatchments();
		}
		
		//catchment relationships
		if (availableOutputs.contains(OutputType.UNIQUE_SUBCATCHMENTS_RELATION)) {
			computeUniqueSubCatchmentRelationship();
		}

		return new PourpointOutput(this);
	}
	
	
	public List<Pourpoint> getPoints(){
		return this.points;
	}
	
	public Double[][] getPourpointMinDistanceMatrix(){
		if (!availableOutputs.contains(OutputType.DISTANCE_MIN) ) return null;
		return getPourpointDistanceMatrix(true);
	}
	public Double[][] getPourpointMaxDistanceMatrix(){
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
	public Integer[][] getPourpointRelationshipMatrix(){
		if (!availableOutputs.contains(OutputType.PP_RELATIONSHIP)) return null;
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
	
	public List<UniqueSubCatchment> getSortedUniqueSubCatchments(){
		if (!availableOutputs.contains(OutputType.UNIQUE_SUBCATCHMENTS) && !availableOutputs.contains(OutputType.UNIQUE_SUBCATCHMENTS_RELATION)) return null;
		Set<UniqueSubCatchment> allCatchments = new HashSet<>();
		for (Pourpoint p : points) {
			allCatchments.addAll(p.getUniqueSubCatchments());
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
	public Integer[][] getUniqueSubCatchmentRelationship(){
		if (!availableOutputs.contains(OutputType.UNIQUE_SUBCATCHMENTS_RELATION)) return null;
		List<UniqueSubCatchment> ordered = getSortedUniqueSubCatchments();
		Integer[][] values = new Integer[ordered.size()][ordered.size()];
		for (int i = 0 ; i < ordered.size(); i ++) {
			for (int j = 0 ; j < ordered.size(); j ++) {
				if (i == j) { values[i][j] = null; continue; }
				
				UniqueSubCatchment pi = ordered.get(i);
				UniqueSubCatchment pj = ordered.get(j);
				if (pi.isDownstream(pj)) {
					values[i][j] = 1;
				}else if (pj.isDownstream(pi)) {
					values[i][j] = -1;
				}else {
					values[i][j] = null;
				}
			}
		}
		return values;
	}
	
	
	private void computeUniqueSubCatchmentRelationship() {
		HashMap<EFlowpath, UniqueSubCatchment> flowToCatchments = new HashMap<>();
		for(Pourpoint p : points) {
			for (UniqueSubCatchment cat : p.getUniqueSubCatchments()) {
				for (ECatchment ecat : cat.getCatchments()) {
					for (EFlowpath flow : ecat.getFlowpaths()) {
						flowToCatchments.put(flow, cat);
					}
				}
			}
		}
				
		for(Pourpoint p : points) {
			
			for (UniqueSubCatchment cat : p.getUniqueSubCatchments()) {
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
							cat.addDownstreamCatchment(flowToCatchments.get(o));
						}
					}
				}
				
			}
		}
	}
	
	
	private void computeUpstreamDownstreamPourpointRelationship() {
		//if (points.size() <2 ) return;
//		HashMap<EFlowpath, Set<Pourpoint>> flowpathToUpPp = new HashMap<>();
		
		HashMap<Nexus, HashMap<Pourpoint, Range>> nodedistances = new HashMap<>();
		distanceValues = new HashMap<>();
		for (Pourpoint pp : points) {
			//walk downstream until we reach the network terminus
			for (EFlowpath edge : pp.getDownstreamFlowpaths()) {
				//for(EFlowpath out : edge.getToNode().getDownFlows()) {
					processPourpointRel(pp, edge, nodedistances);
				//}
			}
		}
		
//		for (Entry<Nexus, HashMap<Pourpoint, Range>> map : nodedistances.entrySet()) {
//			System.out.println(map.getKey().getId() + " " + GeotoolsGeometryReprojector.reproject(map.getKey().getPoint(), BasicTestSuite.TEST_DATA_SRID));
//			
//			HashMap<Pourpoint, Range> r = map.getValue();
//			for (Entry<Pourpoint,Range> d : r.entrySet()) {
//				System.out.println(d.getKey().getId() + ":" + d.getValue().getMinDistance() + " to " + d.getValue().getMaxDistance());
//			}
//			
//		}
		distanceValues = new HashMap<>();
		
		for (HashMap<Pourpoint, Range> nodeRel : nodedistances.values()) {
			for (Pourpoint p : nodeRel.keySet()) {
				Range d = nodeRel.get(p);
				if (d.getMinDistance() == -1) {
					HashSet<Pourpoint> items = new HashSet<>(nodeRel.keySet());
//					items.remove(p);
					//also remove other -1 items as these have the same headwaters
					for (Entry<Pourpoint, Range> rr : nodeRel.entrySet()) {
						if (rr.getValue().getMinDistance() == -1) items.remove(rr.getKey());
					}
					p.getUpstreamPourpoints().addAll(items);
					items.forEach(pr->pr.getDownstreamPourpoints().add(p));
				}
			}
		}
		for (HashMap<Pourpoint, Range> nodeRel : nodedistances.values()) {
			for (Pourpoint p : nodeRel.keySet()) {
				Range d = nodeRel.get(p);
				if (d.getMinDistance() == 0) {
					for (Pourpoint other : p.getUpstreamPourpoints()) {
						PourpointKey key = new PourpointKey(other, p);
						Range r = distanceValues.get(key);
						if (r == null) {
							r = new Range();
							distanceValues.put(key, r);
						}
						r.updateDistance( nodeRel.get(other).getMinDistance(), nodeRel.get(other).getMaxDistance());
						
					}
				}
				
				
			}
		}
	}
	
	private void processPourpointRel(Pourpoint point, EFlowpath path, HashMap<Nexus, HashMap<Pourpoint, Range>> nodedistances) {
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
					outRange.updateDistance(v.minDistance + distance, v.maxDistance+distance);
					
				}
				outValue.put(point, outRange);
			}
			
			for (EFlowpath downstream: outNode.getDownFlows()) {
				toProcess.addLast(downstream);
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
		
		for (ECatchment e : point.getUniqueCatchments()) {
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
			UniqueSubCatchment ppc = catchmentMapping.get(e);
			if (ppc == null) {
				ppc = new UniqueSubCatchment(point, removeHoles);
				catchmentMapping.put(e,ppc);
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
		
		List<ECatchment> tomerge = new ArrayList<>(point.getDownstreamFlowpaths().size());
		for (EFlowpath p : point.getDownstreamFlowpaths()) {
			tomerge.add(p.getCatchment());
		}
		UniqueSubCatchment root = catchmentMapping.get(tomerge.get(0));
		for (int i = 1; i < tomerge.size(); i ++) {
			root.mergeCatchment(catchmentMapping.get(tomerge.get(i)));
		}
		for (ECatchment e : root.getCatchments()) catchmentMapping.put(e, root);
		
		Set<UniqueSubCatchment> items = new HashSet<>();
		items.addAll(catchmentMapping.values());
		point.setUniqueSubCatchments(items);
		
	}
	
	private List<ECatchment>[] findUpstreamCatchments(EFlowpath root, HashMap<EFlowpath, Set<ECatchment>> catchments){
		
		List<ECatchment> uniqueCatchments = new ArrayList<ECatchment>();
		List<ECatchment> otherCatchments = new ArrayList<ECatchment>();
		
		uniqueCatchments.add(root.getCatchment());
		
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
			minDistance = Double.MAX_VALUE;
			maxDistance = Double.MIN_VALUE;
		}
		
		public void updateDistance(double min, double max) {
			if (min < minDistance) minDistance = min;
			if (max > maxDistance) maxDistance = max;
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
