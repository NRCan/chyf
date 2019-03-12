/*
 * Copyright 2019 Government of Canada
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
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
import java.util.stream.Collectors;

import org.apache.commons.collections.iterators.IteratorChain;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.enumTypes.CatchmentType;
import net.refractions.chyf.enumTypes.FlowpathType;
import net.refractions.chyf.enumTypes.NexusType;
import net.refractions.chyf.enumTypes.Rank;
import net.refractions.chyf.hygraph.DrainageArea;
import net.refractions.chyf.hygraph.ECatchment;
import net.refractions.chyf.hygraph.ECatchment.ECatchmentStat;
import net.refractions.chyf.hygraph.EFlowpath;
import net.refractions.chyf.hygraph.HyGraph;
import net.refractions.chyf.hygraph.Nexus;
import net.refractions.chyf.hygraph.StatisticMerger;

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
		OUTPUT_PP("op", "Pourpoints"),	
		DISTANCE_MIN("opdmin", "Pourpoint Minimum Distance Matrix"), 
		DISTANCE_MAX("opdmax", "Pourpoint Maximum Distance Matrix"),
		DISTANCE_PRIMARY("opdprimary", "Pourpoint Primary Distance Matrix"),
		PRT("prt", "Pourpoint Relationship Tree"),
		CATCHMENTS("c", "Catchments"), 
		CATCHMENT_CONTAINMENT("ccr", "Catchment Containment Relationships"), 
		SUBCATCHMENTS("sc", "Subcatchments"), 
		SUBCATCHMENT_RELATIONSHIP("scr", "Subcatchment Flow Relationships"), 
		PARTITIONED_CATCHMENTS("pc", "Partitioned Catchments"), 
		PARTITIONED_CATCHMENT_RELATION("pcr", "Partitioned Catchment Flow Relationships"),
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
	
	private String pointRelationshipTree;
	
	private int prtNodeCounter = 1;
	
	public PourpointEngine(List<Pourpoint> points, HyGraph hygraph) {
		this(points, hygraph, false);
	}
	
	public PourpointEngine(List<Pourpoint> points, HyGraph hygraph, boolean removeHoles) {
		this.points = points;
		this.hygraph = hygraph;
		this.removeHoles = removeHoles;
	}
	
	public HyGraph getGraph() {
		return this.hygraph;
	}
	
	public Set<OutputType> getAvailableOutputs(){
		return this.availableOutputs;
	}
	
	public boolean getRemoveHoles() {
		return this.removeHoles;
	}
	
	public String getPointRelationshipTree() {
		return this.pointRelationshipTree;
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
				Double area = 0.0;
			
				StatisticMerger merger = new StatisticMerger();
				Polygon p = (Polygon) ((MultiPolygon)g).getGeometryN(i);
				for (ECatchment c : holes) {
					if (p.contains( c.getPolygon().getInteriorPoint() )) {
						area += c.getArea();
						merger.addCatchment(c);
					}
					
				}
				DrainageArea da = new DrainageArea(p, area);
				for (Entry<ECatchmentStat,Double> ss : merger.getMergedStats().entrySet()) da.setStat(ss.getKey(), ss.getValue());
				dholes.add( da );
			}
			
			return dholes;
		}else {
			double area = 0;
			StatisticMerger merger = new StatisticMerger();
			for (ECatchment c : holes) {
				area += c.getArea();
				merger.addCatchment(c);
			}
			DrainageArea da = new DrainageArea(g, area);
			for (Entry<ECatchmentStat,Double> ss : merger.getMergedStats().entrySet()) da.setStat(ss.getKey(), ss.getValue());
			return Collections.singleton(da);
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
		if (containsOutput(OutputType.SUBCATCHMENT_RELATIONSHIP,
				OutputType.SUBCATCHMENTS,
				OutputType.PARTITIONED_CATCHMENTS,
				OutputType.PARTITIONED_CATCHMENT_RELATION,
				OutputType.DISTANCE_MAX,
				OutputType.DISTANCE_PRIMARY,
				OutputType.DISTANCE_MIN,
				OutputType.INTERIOR_CATCHMENT,
				OutputType.PRT)) {
			computeUpstreamDownstreamPourpointRelationship();
		}
		
		//catchments for pourpoints
		if (containsOutput(OutputType.CATCHMENTS,
				OutputType.SUBCATCHMENTS,
				OutputType.PARTITIONED_CATCHMENTS,
				OutputType.PARTITIONED_CATCHMENT_RELATION,
				OutputType.INTERIOR_CATCHMENT)) {
			computeUniqueCatchments();
		}
			
			
		if (availableOutputs.contains(OutputType.CATCHMENT_CONTAINMENT) || removeHoles) {
			computeCatchmentContainsRelationship();
		}
			
		//catchment relationships
		if (availableOutputs.contains(OutputType.PARTITIONED_CATCHMENT_RELATION)) {
			computePartitionedCatchmentRelations();
		}
	
		if (containsOutput(OutputType.INTERIOR_CATCHMENT) || 
			 (removeHoles && containsOutput(OutputType.CATCHMENTS, OutputType.SUBCATCHMENTS, OutputType.PARTITIONED_CATCHMENTS))) {
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
	
	public Double[][] getProjectedPourpointPrimaryDistanceMatrix(){
		if (!availableOutputs.contains(OutputType.DISTANCE_PRIMARY) ) return null;
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
				if (range == null || range.primaryDistance < 0) {
					values[i][j] = null;	
				}else {
					values[i][j] = range.primaryDistance * offset;
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
	public Integer[][] getSubCatchmentRelationship(){
		if (!availableOutputs.contains(OutputType.SUBCATCHMENT_RELATIONSHIP)) return null;
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
	
	public List<UniqueSubCatchment> getSortedPartitionedCatchments(){
		if (!availableOutputs.contains(OutputType.PARTITIONED_CATCHMENTS) && !availableOutputs.contains(OutputType.PARTITIONED_CATCHMENT_RELATION) ) return null;
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
	public Integer[][] getPartitionedCatchmentRelationship(){
		if (!availableOutputs.contains(OutputType.PARTITIONED_CATCHMENT_RELATION)) return null;
		List<UniqueSubCatchment> ordered = getSortedPartitionedCatchments();
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
	
	
	private void computePartitionedCatchmentRelations() {
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
		
		//in the double array the first value is the
		//primary distance to the node and the second
		//value the horton order of the input edge
		HashMap<EFlowpath, HashMap<Pourpoint, Double>> primarydistances = new HashMap<>();

		distanceValues = new HashMap<>();
		
		for (Pourpoint pp : points) {
			//walk downstream until we reach the network terminus
			for (EFlowpath edge : pp.getDownstreamFlowpaths()) {
				processPourpointRel(pp, edge, nodedistances, primarydistances);
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
		
		HashMap<Nexus, HashMap<Pourpoint, Double>> prjprimarydistances = new HashMap<>();
		for (Entry<EFlowpath, HashMap<Pourpoint, Double>> e : primarydistances.entrySet()) {
			Nexus n = e.getKey().getToNode();
			HashMap<Pourpoint, Double> items = prjprimarydistances.get(n);
			if (items == null) {
				items = new HashMap<>();
				prjprimarydistances.put(n,  items);
			}
			items.putAll(e.getValue());
		}

		for (HashMap<Pourpoint, Double> nodeRel : prjprimarydistances.values()) {
			for (Pourpoint p : nodeRel.keySet()) {
				Double d = nodeRel.get(p);
				if (d == 0) {
					//for all other pourpoints at this node
					for (Pourpoint p2 : nodeRel.keySet()) {	
						PourpointKey key = new PourpointKey(p, p2);
						Double primarydistance = nodeRel.get(p2);
						Range r = distanceValues.get(key);
						if (r == null) {
							r = new Range();
							distanceValues.put(key, r);
						}
						r.primaryDistance = primarydistance;
					}
				}
			}
		}
		
		if (availableOutputs.contains(OutputType.PRT)) {
			computePointRelationshipTree2(primarydistances);
		}
	}
	
	private void computePointRelationshipTree2(HashMap<EFlowpath, HashMap<Pourpoint, Double>> primarydistances) {

		List<Pourpoint> down = new ArrayList<>();
		for (Pourpoint p : points) {
			if (p.getDownstreamPourpoints().isEmpty()) {
				down.add(p);
			}
		}
		
		StringBuilder frt = new StringBuilder();
		
		ArrayList<EFlowpath> toprocess = new ArrayList<>();
		for(Pourpoint p : down) {
			toprocess.addAll(p.getDownstreamFlowpaths());
		}
		
		//if to process is greater than 1 we need to find the most downstream edge
		//that all pourpoint converge into
		ArrayList<EFlowpath> toprocess2 = null;
		if (toprocess.size() <= 1) {
			toprocess2 = toprocess;
		}else {
			toprocess2 = new ArrayList<>();
			Set<Pourpoint> matched = new HashSet<>(points);
			
			EFlowpath current = toprocess.get(0);
			while(true) {
				
				HashMap<Pourpoint, Double> ed = primarydistances.get(current);
				if (ed.keySet().size() ==  matched.size()) {
					//make sure edges is not a start edge
					boolean ok = true;
					for (Pourpoint p : matched) {
						if (p.getDownstreamFlowpaths().contains(current)) {
							ok = false;
							break;
						}
					}
					if (ok) {
						//we have a flopath to which everything converges
						toprocess2.add(current);
						break;
					}
				}
				
				//try the next downstream one
				EFlowpath out = null;	
				for (EFlowpath d : current.getToNode().getDownFlows()) {
					if (d.getRank() == Rank.PRIMARY) {
						out = d;
						break;
					}
				}
				if (out == null) {
					toprocess2.add(current);
					for (Pourpoint p : ed.keySet()) {
						toprocess.removeAll(p.getDownstreamFlowpaths());
						matched.remove(p);
					}
					if (toprocess.isEmpty()) break;
					current = toprocess.get(0);
				}else {
					current = out;
				}
			}	
		}
		
		for (EFlowpath p : toprocess2) {
			StringBuilder sb = new StringBuilder();
			processEdge2(p, sb, primarydistances, false);
			frt.append(sb.toString());
			frt.append(";");	
		}
		frt.deleteCharAt(frt.length() - 1);
		this.pointRelationshipTree = frt.toString();
	}
	
	private void appendId (StringBuilder sb, String id) {
		if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '(') {
			sb.append(",");
		}
		sb.append(id);
	}
	
	private void processEdge2(EFlowpath edge, StringBuilder frt, HashMap<EFlowpath,HashMap<Pourpoint, Double>> primarydistances, boolean ignorestart) {
		
		List<Set<Pourpoint>> upEdges = new ArrayList<>();
		List<EFlowpath> importantUp = new ArrayList<>();
		
		Pourpoint startPnt = null;
		
		//find if I represent a pourpoint start point
		HashMap<Pourpoint, Double> map = primarydistances.get(edge);
		for (Entry<Pourpoint, Double> d : map.entrySet()) {
			if ( d.getValue() == 0) {
				startPnt = d.getKey();
			}
		}

		//find all inflows and determine if they represent the same pourpoint
		//set or if I need an x node
		for (EFlowpath in : edge.getFromNode().getUpFlows()) {
			if (primarydistances.containsKey(in)) {
				upEdges.add(primarydistances.get(in).keySet());
				importantUp.add(in);
			}
		}
		boolean issame = true;
		for (int i = 0; i < upEdges.size(); i ++) {
			for (int j = i; j < upEdges.size(); j ++) {
				if (!setEqual(upEdges.get(i), upEdges.get(j))) {
					issame = false;
					break;
				}
			}
		}
		
		//in the special case where a pourpoint represents
		//all upstream edges (ccode 0) we need to do things a big different
		Pourpoint ccode0 = null;
		if (upEdges.size() > 1) {
			for (Pourpoint p : upEdges.get(0)) {
				if (p.getDownstreamFlowpaths().size() > 1 && p.getDownstreamFlowpaths().contains(importantUp.get(0))) {
					ccode0 = p;
					break;
				}
			}
		}

		int close = 0;
		//this represents a start point - make a node for it
		if (!ignorestart && startPnt != null) {
			appendId(frt, startPnt.getId());
			if (!importantUp.isEmpty()) {
				frt.append("(");
				close++;
			}
		}
		
		//in this case we also need a node here
		if (ccode0 != null) {
			appendId(frt, ccode0.getId());
			if (!issame) {
				frt.append("(");
				close++;
			}
		}else {
			//if they have different upstream areas we need and x node
			if (!issame) {
				appendId(frt, "x" + prtNodeCounter++);
				frt.append("(");
				close++;
			}
		}
		
		
		//sort these on horton order
		importantUp.sort((a,b)  -> {
			Integer a1 = a.getHortonOrder();
			if (a1 == null) a1 = -1;
			Integer b1 = b.getHortonOrder();
			if (b1 == null) b1 = -1;
			return a1.compareTo(b1);
		});
		for (EFlowpath i : importantUp) {
			processEdge2(i, frt, primarydistances, ccode0 != null);
			
		}	
		
		for (int i = 0; i < close; i ++){
			frt.append(")");
		}
	}
	
	private void processPourpointRel(Pourpoint point, EFlowpath path, HashMap<Nexus, HashMap<Pourpoint, Range>> nodedistances, HashMap<EFlowpath, HashMap<Pourpoint, Double>> primaryDistances) {
		//TODO: stop once we've visited all pourpoints as there won't be any more relationships downstream
		//this stop would only be useful if all the pourpoints are on the same network, if they are on
		//different trees it might not be useful
		ArrayDeque<EFlowpath> toProcess = new ArrayDeque<>();
		toProcess.add(path);
		
		
		//this is the first edges; we don't care if its primary or not
		HashMap<Pourpoint, Double> primaryOutValue = primaryDistances.get(path);
		if (primaryOutValue == null) {
			primaryOutValue = new HashMap<>();
			primaryDistances.put(path,  primaryOutValue);
		}
		primaryOutValue.put(point, 0.0);
		
		
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
			//update primary distance
			if (item.getRank() == Rank.PRIMARY && item != path) {
				//find the upstream edge
				EFlowpath up = null;
				for (EFlowpath upedges : item.getFromNode().getUpFlows()) {
					if (primaryDistances.containsKey(upedges) && primaryDistances.get(upedges).containsKey(point)) {
						up = upedges;
						break;
					}
				}
				
				if (up != null) {
					Double pdistance = primaryDistances.get(up).get(point);
					pdistance += distance;
				
					primaryOutValue = primaryDistances.get(item);
					if (primaryOutValue == null) {
						primaryOutValue = new HashMap<>();
						primaryDistances.put(item,  primaryOutValue);
					}
					primaryOutValue.put(point, pdistance);
				}
			}
			
			
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
					//but we always continue down the primary path
				}
				outValue.put(point, outRange);
			}
			
			if (item.getRank() == Rank.PRIMARY || continueDownstream) {
				for (EFlowpath downstream: outNode.getDownFlows()) {
					toProcess.addLast(downstream);
				}
			}
		}	
	}
	
	
	/**
	 * Computes the unique catchments and unique subcatchments for each pourpoint
	 * @throws Exception 
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
				List<ECatchment>[] results = findUpstreamCatchments(item, path, catchments);
					
				List<ECatchment> uniqueCatchments = results[0];
				List<ECatchment> otherCatchments = results[1];
					
				item.addUpstreamCatchments(uniqueCatchments, otherCatchments);
				
				Set<ECatchment> all = new HashSet<>();
				all.addAll(uniqueCatchments);
				all.addAll(otherCatchments);
				catchments.put(path, all);
			}
			
			for (EFlowpath path : item.getOtherDownstreamFlowpaths()) {
				List<ECatchment>[] results = findUpstreamCatchments(item, path, catchments);
				List<ECatchment> uniqueCatchments = results[0];
				List<ECatchment> otherCatchments = results[1];
				item.addNeighbourCatchments(uniqueCatchments, otherCatchments);
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
		
		//compute stop catchments for merging elementrary catchments into
		//traversal compliant catchments
		//add catchments inflow catchments do not match outflow catchments
		Set<ECatchment> stopPoints = new HashSet<>();
		
		HashMap<ECatchment, Set<Pourpoint>> mappings = new HashMap<>();
		HashMap<Pourpoint, Pourpoint> othermapping = new HashMap<>();
		for (Pourpoint p : points) {
			IteratorChain ic = new IteratorChain(p.getUniqueCatchments().iterator(), p.getSharedCatchments().iterator());
			ic.forEachRemaining(c->{
				Set<Pourpoint> m = mappings.get(c);
				if (m == null) {
					m = new HashSet<>();
					mappings.put((ECatchment)c, m);
				}
				m.add(p);
			});
			
			//these are the neighbor upstream areas of the pourpoint
			p.getNeighbourUpstreamCatchments().forEach(c->{
				Set<Pourpoint> m = mappings.get(c);
				if (m == null) {
					m = new HashSet<>();
					mappings.put((ECatchment)c, m);
				}
				Pourpoint other = othermapping.get(p);
				if (other == null) {
					other = new Pourpoint(p.getPoint(),p.getCcode(),p.getId() + "*neighbour");
					othermapping.put(p, other);
				}
				m.add(other);
			});
		}
		//at each nexus check upstream and downstream catchments
		Set<Nexus> done = new HashSet<>();
		for (ECatchment c : mappings.keySet()) {
			done.addAll(c.getUpNexuses());
			done.addAll(c.getDownNexuses());
		}
		for (Nexus d : done) {
			List<Set<Pourpoint>> inFlowPourpoints = new ArrayList<>();
			for (EFlowpath in : d.getUpFlows()) {
				Set<Pourpoint> pntsAtNexus = mappings.get(in.getCatchment());	
				if (pntsAtNexus != null) inFlowPourpoints.add(pntsAtNexus);
			}
			for (EFlowpath out : d.getDownFlows()) {
				Set<Pourpoint> pntsAtNexus = mappings.get(out.getCatchment());	
				if (pntsAtNexus == null) continue;
				for (Set<Pourpoint> set1 : inFlowPourpoints) {
					if (!setEqual(set1, pntsAtNexus)) {
						stopPoints.add(out.getCatchment());
						break;
					}
				}
			}
		}
		
		Set<ECatchment> allcatchments = mappings.keySet();
		Set<ECatchment> usedcatchments = new HashSet<>();
		points.forEach(p->createPartitionedCatchments(p, stopPoints, usedcatchments, allcatchments));
		
		//assign an unique identifier
		int id = 0;
		for (Pourpoint p : points) {
			HashMap<UniqueSubCatchment, Double> areas = new HashMap<>();
			for (UniqueSubCatchment i : p.getTraversalCompliantCatchments()) {
				double d1 = 0;
				for (ECatchment d : i.getCatchments()) d1+= d.getArea();
				areas.put(i, d1);
			}
			List<UniqueSubCatchment> sort = new ArrayList<>(p.getTraversalCompliantCatchments());
			sort.sort((a,b)->{
				double d1 = areas.get(a);
				double d2 = areas.get(b);
				if (d1 > d2) return -1;
				if (d1 > d2) return 1;
				return 0;
				
			});
			for (UniqueSubCatchment s : sort) {
				s.setId(generateId(id++));
			}
		}
	}
	
	private String generateId(int i) {
		if (i < 0 ) return "";
		return generateId( (i / 26) - 1 ) + (char)(65 + i%26);
	}
	
	/**
	 * validates if two sets are equal
	 * @param set1
	 * @param set2
	 * @return
	 */
	private boolean setEqual(Set<?> set1, Set<?> set2) {
		if (set1.size() != set2.size()) return false;
		for (Object x : set1) {
			if (!set2.contains(x)) return false;
		}
		return true;
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
	 * Computes the partitioned catchments
	 */
	private void createPartitionedCatchments(Pourpoint point, Set<ECatchment> stopPoints, Set<ECatchment> processed, Set<ECatchment> allcatchments) {
		HashMap<ECatchment, UniqueSubCatchment> catchmentMapping = new HashMap<>();

		for (ECatchment e : point.getUniqueCatchments()) {
			//ensure they are unique; if it has already been processed skip it
			if (processed.contains(e)) {
				//already processed; add to existing catchment for id purposes
				for (Pourpoint other : points) {
					if (other == point || other.getTraversalCompliantCatchments() == null) continue;
					for (UniqueSubCatchment cc : other.getTraversalCompliantCatchments()) {
						if (cc.getCatchments().contains(e)) cc.addPoint(point);
					}
				}
				continue;
			}
			processed.add(e);


			UniqueSubCatchment ppc = catchmentMapping.get(e);
			if (ppc == null) {
				ppc = new UniqueSubCatchment(point);
				catchmentMapping.put(e,ppc);
			}
			ppc.addCatchment(e);
			//this has no flows and should be merged with whatever catchment it flows into
			if (e.getType() == CatchmentType.BANK)  continue;
			
			if(stopPoints.contains(e)) continue;
			
			//merge with upstream catchments
			List<ECatchment> upCatchments2 = new ArrayList<>();
			for (Nexus upN : e.getUpNexuses()) {
				//only add up catchments that are used in the analysis (required as a result of non-broken dlr catchments)
				if (upN.getType() == NexusType.BANK) {
					if (allcatchments.contains(upN.getBankCatchment())) upCatchments2.add(upN.getBankCatchment());
				}else {
					for (EFlowpath inFlow : upN.getUpFlows()) {
						if (allcatchments.contains(inFlow.getCatchment())) upCatchments2.add(inFlow.getCatchment());
					}
				}
			}
			//combine this catchment with the unique catchments that are directly upstream
			for (ECatchment up : upCatchments2) {
				if (catchmentMapping.containsKey(up)) ppc.mergeCatchment(catchmentMapping.get(up));
				ppc.addCatchment(up);
			}
			for (ECatchment upc : ppc.getCatchments()) {
				catchmentMapping.put(upc, ppc);
			}			
		}
		
		//add bank catchments
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
		
		//merge catchments that are siblings at stop points
		for (ECatchment c1 : stopPoints) {
			for (Nexus d : c1.getUpNexuses()) {
				List<EFlowpath> inflows = new ArrayList<>(d.getUpFlows());
				for (int i = 0; i < inflows.size(); i ++) {
					for (int j = i; j < inflows.size(); j ++) {
						ECatchment m1 = inflows.get(i).getCatchment();
						ECatchment m2 = inflows.get(j).getCatchment();
						UniqueSubCatchment u1 = catchmentMapping.get(m1);
						UniqueSubCatchment u2 = catchmentMapping.get(m2);
						if (u1 != null && u2 != null) {
							u1.mergeCatchment(u2);
							for (ECatchment upc : u1.getCatchments()) {
								catchmentMapping.put(upc, u1);
							}
						}
					}
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
		
		//merge together all the upstream catchments for the pourpoint (ccode 0)
		UniqueSubCatchment root = catchmentMapping.get(tomerge.get(0));
		if (root == null) {
			return;
			//throw new PourpointException("Could not compute Traversal Compliant Catchments.  Ensure pourpoints are not projected to same catchment.");
		}
		for (int i = 1; i < tomerge.size(); i ++) {
			root.mergeCatchment(catchmentMapping.get(tomerge.get(i)));
		}
		
		//create a unique set of the subcatchments
		Set<UniqueSubCatchment> items = new HashSet<>();
		items.addAll(catchmentMapping.values());
		point.setTraversalCompliantCatchments(items);
	}
	
	
	private List<ECatchment>[] findUpstreamCatchments(Pourpoint point, EFlowpath root, HashMap<EFlowpath, Set<ECatchment>> catchments){
		
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
				//TODO: c should never be null here - this fix is added temporarily for KOTL data issues.
				if (c != null) uniqueCatchments.add(c);
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
			DrainageArea area = point.getCatchmentDrainageArea(hygraph, false);
			
			//TODO: this should always be a single polygon; this was added because of issues
			//with kotl dataset and multipolygons were returned
			for (int i = 0; i < area.getGeometry().getNumGeometries(); i ++) {
				Polygon p = ((Polygon)area.getGeometry().getGeometryN(i));
				geometries.add(p);
			}
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
				if (p.getSharedCatchments().contains(closest.getCatchment())) {
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
		double primaryDistance = -1;
		
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
		public double getPrimaryDistance() { return this.primaryDistance; }
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
	
	
	class NexusItem{
		Nexus nexus;
		double distance;
		int hortonorder;
	}
}
