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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.union.UnaryUnionOp;

import net.refractions.chyf.enumTypes.NexusType;
import net.refractions.chyf.hygraph.DrainageArea;
import net.refractions.chyf.hygraph.ECatchment;
import net.refractions.chyf.hygraph.EFlowpath;
import net.refractions.chyf.hygraph.HyGraph;
import net.refractions.chyf.hygraph.Nexus;



public class PourpointEngine {

	private List<Pourpoint> points;
	private HyGraph hygraph;
	
	private boolean removeHoles = false;
	
	private HashMap<PourpointKey, Range> distanceValues = new HashMap<>();
	
	public static int TEST_DATA_SRID = 4617;
	
	public PourpointEngine(List<Pourpoint> points, HyGraph hygraph) {
		this.points = Collections.unmodifiableList(points);
		this.hygraph = hygraph;
	}
	
	public PourpointOutput compute() {
		//compute downstream flowpaths for pourpoints
		points.forEach(p->p.findDownstreamFlowpaths(hygraph));
		
		//compute pourpoint relationship & distances between points
		computeUpstreamDownstreamPourpointRelationship();
		
		//catchments for pourpoints
		computeUniqueCatchments();
		
		//catchment relationships
		computeUniqueSubCatchmentRelationship();

		return new PourpointOutput(this);
	}
	
	
	public List<Pourpoint> getPoints(){
		return this.points;
	}
	
	public Double[][] getPourpointMinDistanceMatrix(){
		return getPourpointDistanceMatrix(true);
	}
	public Double[][] getPourpointMaxDistanceMatrix(){
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
		Set<UniqueSubCatchment> allCatchments = new HashSet<>();
		for (Pourpoint p : points) {
			allCatchments.addAll(p.getUniqueCombinedCatchments());
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
	public Integer[][] getPourpointCatchmentRelationship(){
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
			for (UniqueSubCatchment cat : p.getUniqueCombinedCatchments()) {
				for (ECatchment ecat : cat.getCatchments()) {
					for (EFlowpath flow : ecat.getFlowpaths()) {
						flowToCatchments.put(flow, cat);
					}
				}
			}
		}
				
		for(Pourpoint p : points) {
			
			for (UniqueSubCatchment cat : p.getUniqueCombinedCatchments()) {
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

		HashMap<EFlowpath, Set<Pourpoint>> flowpathToUpPp = new HashMap<>();
		
		HashMap<Nexus, HashMap<Pourpoint, Double>> nodedistances = new HashMap<>();
		distanceValues = new HashMap<>();
		for (Pourpoint pp : points) {
			//walk downstream until we reach the network terminus
			for (EFlowpath edge : pp.getDownstreamFlowpaths()) {
				if (flowpathToUpPp.containsKey(edge)) continue;
				processEdge(edge, Collections.singleton(pp), flowpathToUpPp, nodedistances);
			}
		}
		
		for (Pourpoint pp : points) {
			for (EFlowpath down : pp.getDownstreamFlowpaths()) {
				Set<Pourpoint> pps = flowpathToUpPp.get(down);
				if (pps == null) continue;
				pps.remove(pp);
				pp.getUpstreamPourpoints().addAll(pps);
			}
		}
				
		for (Pourpoint pp : points) {
			for (Pourpoint up : pp.getUpstreamPourpoints()) {
				up.getDownstreamPourpoints().add(pp);
			}
		}
	}
	
	private void processEdge(EFlowpath path, Set<Pourpoint> upstream, HashMap<EFlowpath, Set<Pourpoint>> flowpathToUpPp,
			HashMap<Nexus, HashMap<Pourpoint, Double>> nodedistances) {
		
		HashMap<Pourpoint, Double> values = nodedistances.get(path.getFromNode());
		if (values != null) {
			double distance = path.getLength();
			//compute distances for downstream node
			HashMap<Pourpoint, Double> downValues = new HashMap<>();
			for (Entry<Pourpoint, Double> e : values.entrySet()) {
				downValues.put(e.getKey(),  e.getValue() + distance);
			}
			nodedistances.put(path.getToNode(), downValues);
		}else {
			values = new HashMap<>();
			nodedistances.put(path.getToNode(), values);
		}
				
		
		Set<Pourpoint> pointsOnEdge = new HashSet<>();
		for (Pourpoint pp : points) {
			for (EFlowpath dpath : pp.getDownstreamFlowpaths()) {
				if (dpath.equals(path)) {
					pointsOnEdge.add(pp);
					break;
				}
			}
		}
		for (Pourpoint pp : pointsOnEdge) {
			for (Entry<Pourpoint, Double> e : values.entrySet()) {
				//update pourpoint distances
				Pourpoint uppp = e.getKey();
				PourpointKey key = new PourpointKey(uppp, pp);
				Range dd = distanceValues.get(key);
				if (dd == null) {
					dd = new Range();
					distanceValues.put(key, dd);
				}
				dd.updateDistance(e.getValue() + path.getLength());
			}
			
			values.put(pp, 0d);
		}
		
		pointsOnEdge.addAll(upstream);
		Set<Pourpoint> temp = flowpathToUpPp.get(path);
		if (temp == null) {
			temp = new HashSet<>();
			flowpathToUpPp.put(path, temp);
		}
		temp.addAll(pointsOnEdge);

		for (EFlowpath downstream: path.getToNode().getDownFlows()) {
			processEdge(downstream, pointsOnEdge, flowpathToUpPp, nodedistances);
		}
		nodedistances.remove(path.getToNode());
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
				ppc = new UniqueSubCatchment(point);
				catchmentMapping.put(e,ppc);
			}
			for (Pourpoint pp : inpoints) ppc.addUpstreamPourpoint(pp);
			
			if (inpoints.isEmpty()) {
				//combine this catchment with the unique catchments that are directly upstream
				List<ECatchment> upCatchments = new ArrayList<>();
				upCatchments.add(e);
				for (Nexus upN : e.getUpNexuses()) {
					for (EFlowpath inFlow : upN.getUpFlows()) {
						if (point.getUniqueCatchments().contains(inFlow.getCatchment())) {
							upCatchments.add(inFlow.getCatchment());
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
		point.setUniqueCombinedCatchments(items);
		
	}
	
	private List<ECatchment>[] findUpstreamCatchments(EFlowpath root, HashMap<EFlowpath, Set<ECatchment>> catchments){
		
		List<ECatchment> uniqueCatchments = new ArrayList<ECatchment>();
		List<ECatchment> otherCatchments = new ArrayList<ECatchment>();
		
		HashSet<ECatchment> resultSet = new HashSet<ECatchment>();

		uniqueCatchments.add(root.getCatchment());
		
		for(int i = 0; i < uniqueCatchments.size(); i++) {
			for(Nexus n: uniqueCatchments.get(i).getUpNexuses()) {
				for(EFlowpath f: n.getUpFlows()) {
					if(f.getCatchment() != null) {
						ECatchment cc = f.getCatchment();
						if (catchments.containsKey(f)) {
							otherCatchments.addAll(catchments.get(f));
						}else {
							if(resultSet.add(cc)) {
								uniqueCatchments.add(cc);
							}
						}
					}
				}
				//TODO: figure this out
				if(n.getType() == NexusType.BANK) {
					if(n.getBankCatchment() != null) {
						ECatchment cc = n.getBankCatchment();
//						if (catchments.containsKey(f)) {
//							otherCatchments.addAll(catchments.get(cc));
//						}else {
							if(resultSet.add(cc)) {
								uniqueCatchments.add(cc);
							}
//						}
					}
				}
			}
		}
		return new List[] {uniqueCatchments, otherCatchments};
	}

	
	class Range{
		double minDistance;
		double maxDistance;
		
		public Range() {
			minDistance = Double.MAX_VALUE;
			maxDistance = Double.MIN_VALUE;
		}
		
		public void updateDistance(double distance) {
			if (distance < minDistance) minDistance = distance;
			if (distance > maxDistance) maxDistance = distance;
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
