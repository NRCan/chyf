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
package net.refractions.chyf.hygraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.enumTypes.CatchmentType;
import net.refractions.chyf.enumTypes.NexusType;
import net.refractions.chyf.hygraph.ECatchment.ECatchmentStat;
import net.refractions.chyf.indexing.BboxIntersectsFilter;
import net.refractions.chyf.indexing.ECatchmentContainsPointFilter;
import net.refractions.chyf.indexing.Filter;
import net.refractions.chyf.indexing.RTree;
import net.refractions.chyf.indexing.SpatiallyIndexable;

public class HyGraph {
	static final Logger logger = LoggerFactory.getLogger(HyGraph.class.getCanonicalName());

	private Nexus[] nexuses;
	private EFlowpath[] eFlowpaths;
	private ECatchment[] eCatchments;
	private RTree<Nexus> nexusIndex;
	private RTree<EFlowpath> eFlowpathIndex;
	private RTree<ECatchment> eCatchmentIndex;

	public HyGraph(Nexus[] nexuses, EFlowpath[] eFlowpaths, ECatchment[] eCatchments) {
		this.nexuses = nexuses;
		this.eFlowpaths = eFlowpaths;
		this.eCatchments = eCatchments;

		// Sort the terminal nodes first for easy access
		// Actually don't as this ruins the id-based array access 
//		Arrays.sort(nexuses, new Comparator<Nexus>(){
//			@Override
//			public int compare(Nexus n1, Nexus n2) {
//				if(n1.getType().equals(NexusType.TERMINAL)
//						&& !n2.getType().equals(NexusType.TERMINAL)) {
//					return -1;
//				} else if(!n1.getType().equals(NexusType.TERMINAL)
//						&& n2.getType().equals(NexusType.TERMINAL)) {
//					return 1;
//				}
//				return 0;
//			}
//		});

		nexusIndex = new RTree<Nexus>(Arrays.asList(nexuses));
		eFlowpathIndex = new RTree<EFlowpath>(Arrays.asList(eFlowpaths));
		eCatchmentIndex = new RTree<ECatchment>(Arrays.asList(eCatchments));
	}
	
	public Nexus getNexus(int nexusId) {
		if(nexusId > 0 && nexusId <= nexuses.length) {
			return nexuses[nexusId-1];
		}
		return null;
	}

	public EFlowpath getEFlowpath(int eflowpathId) {
		if(eflowpathId > 0 && eflowpathId <= eFlowpaths.length) {
			return eFlowpaths[eflowpathId-1];
		}
		return null;
	}

	/*
	 * Returns the flowpath that the give point would flow into,
	 * based on the elementary catchment the point is in.
	 * 
	 * @param point the point to search from
	 * @return the EFlowpath the point would flow into or null if the point
	 * 		is not contained in any elementary catchment
	 */
	public EFlowpath getEFlowpath(Point point) {
		List<ECatchment> possibleCatchments = eCatchmentIndex.search(point, 1, null, 
				new ECatchmentContainsPointFilter(point));
		EFlowpath flowpath = null;
		if(possibleCatchments.size() > 0) {
			ECatchment c = possibleCatchments.get(0);
			List<EFlowpath> possibleFlowpaths = c.getFlowpaths();
			switch(possibleFlowpaths.size()) {
				case 0:
					if(c.getType() == CatchmentType.BANK
							&& c.getDownNexuses().size() == 1) {
						flowpath = c.getDownNexuses().get(0).getDownFlows().get(0);
					}
					break;
				case 1:
					flowpath = possibleFlowpaths.get(0);
					break;
				default:
					// the catchment has multiple flowpaths, find the closest flowpath
					double dist = Double.POSITIVE_INFINITY;
					for(EFlowpath f: possibleFlowpaths) {
						double newDist = f.distance(point); 
						if(newDist < dist) {
							flowpath = f;
							dist = newDist; 
						}
					}
			}
		}
		return flowpath;
	}
	
	public List<EFlowpath> getEFlowpaths(Filter<EFlowpath> filter) {
		List<EFlowpath> results = new ArrayList<EFlowpath>();
		for(EFlowpath f : eFlowpaths) {
			if(filter.pass(f)) {
				results.add(f);
			}
		}
		return results;
	}

	public List<EFlowpath> findEFlowpaths(Point p, int maxResults, Double maxDistance, Filter<EFlowpath> f) {
		return eFlowpathIndex.search(p, maxResults, maxDistance, f);
	}

	public ECatchment getECatchment(int ecatchmentId) {
		if(ecatchmentId > 0 && ecatchmentId <= eCatchments.length) {
			return eCatchments[ecatchmentId-1];
		}
		return null;	
	}

	public ECatchment getECatchment(Point point) {
		List<ECatchment> eCatchments = findECatchments(point, 1, 0.0, new ECatchmentContainsPointFilter(point));
		if(eCatchments.size() > 0) {
			return eCatchments.get(0);
		}
		return null;
	}
	
	/**
	 * Finds all ecatchments with interior intersections with
	 * the given polygon
	 * 
	 * @param p
	 * @return
	 */
	public List<ECatchment> findECatchments(Polygon p) {
		Envelope e = p.getEnvelopeInternal();
		double distance = Math.sqrt( (e.getWidth() * e.getWidth()) + (e.getHeight() * e.getHeight())) / 2;
		BboxIntersectsFilter<ECatchment> filter = new BboxIntersectsFilter<>(e);
		Point center = p.getFactory().createPoint(e.centre());
		
		List<ECatchment> items = new ArrayList<>();
		for (ECatchment cat : eCatchmentIndex.search(center, 1000, distance, filter)) {
			if (cat.getPolygon().relate(p, "2********")) {
				items.add(cat);
			}	
		}
		return items;
	}
	
	/**
	 * Finds all flow paths within the given envelope
	 * @param e
	 * @return
	 */
	public List<EFlowpath> findEFlowpaths(Envelope e, Filter<EFlowpath> filter) {
		double distance = Math.sqrt( (e.getWidth() * e.getWidth()) + (e.getHeight() * e.getHeight())) / 2;
		BboxIntersectsFilter<EFlowpath> bboxfilter = new BboxIntersectsFilter<>(e);
		Point center = eFlowpaths[0].getLineString().getFactory().createPoint(e.centre());
		Filter<EFlowpath> combined = item->filter.pass(item) && bboxfilter.pass(item);
		List<EFlowpath> items = new ArrayList<>();
		for (EFlowpath cat : eFlowpathIndex.search(center, 1000, distance, combined)) {
			items.add(cat);
		}
		return items;
	}
	
	public List<ECatchment> findECatchments(Point p, int maxResults, Double maxDistance, Filter<ECatchment> f) {
		return eCatchmentIndex.search(p, maxResults, maxDistance, f);
	}

	public List<Nexus> findNexuses(Point p, int maxResults, Double maxDistance, Filter<Nexus> f) {
		return nexusIndex.search(p, maxResults, maxDistance, f);
	}

	public List<SpatiallyIndexable> getECatchmentIndexNode(int id) {
		return eCatchmentIndex.getNode(id);
	}

	public List<EFlowpath> getUpstreamEFlowpaths(EFlowpath eFlowpath, int maxResults) {
		if(eFlowpath == null) {
			return Collections.emptyList();
		}
		HashSet<EFlowpath> resultSet = new HashSet<EFlowpath>(maxResults);
		List<EFlowpath> results = new ArrayList<EFlowpath>(maxResults);
		results.add(eFlowpath);
		resultLoop:
			for(int i = 0; i < results.size(); i++) {
				for(EFlowpath upstream: results.get(i).getFromNode().getUpFlows()) {
					if(resultSet.add(upstream)) {
						results.add(upstream);
						if(results.size() >= maxResults) {
							break resultLoop;
						}
					}
				}
			}
		return results;
	}

	/**
	 * 
	 * Given a point, the elementary catchment containing the point is returned. 
	 * If the elementary catchment flows into a flowpath associated with a single-line river, 
	 * then the flowpath is returned; otherwise if the flowpath is associated with a 
	 * polygonal waterbody (e.g., a double-line river or a lake), then the 
	 * polygonal waterbody is returned. This behaviour occurs recursively downstream 
	 * until the limit of the data is reached. This is intended to convey a more 
	 * accurate representation of what actually happens in the case of a spill for example.
	 * 
	 * @param eFlowpath the flowpath to start from 
	 * @param maxResults the maximum number of flowpaths to return
	 * @return a collection of eflowpath or ecatchment objects
	 */
	public Collection<SpatiallyIndexable> getDownstreamMultiDimensional(ECatchment eCatchment, int maxResults) {
		Collection<ECatchment> catchments = getDownstreamECatchments(eCatchment, maxResults);
		return filterMultiDimensionalResults(catchments, maxResults);
	}

	/**
	 * 
	 * Given a point, the elementary catchment containing the point is returned. 
	 * If the elementary catchment flows into a flowpath associated with a single-line river, 
	 * then the flowpath is returned; otherwise if the flowpath is associated with a 
	 * polygonal waterbody (e.g., a double-line river or a lake), then the 
	 * polygonal waterbody is returned. This behaviour occurs recursively upstream 
	 * until the limit of the data is reached. This is intended to convey a more 
	 * accurate representation of what actually happens in the case of a spill for example.
	 * 
	 * @param eFlowpath the flowpath to start from 
	 * @param maxResults the maximum number of flowpaths to return
	 * @return a collection of eflowpath or ecatchment objects
	 */
	public Collection<SpatiallyIndexable> getUpstreamMultiDimensional(ECatchment eCatchment, int maxResults) {
		Collection<ECatchment> catchments = getUpstreamECatchments(eCatchment, maxResults);
		return filterMultiDimensionalResults(catchments, maxResults);
	}
	
	private Collection<SpatiallyIndexable> filterMultiDimensionalResults(Collection<ECatchment> catchments, int maxResults){
		Set<SpatiallyIndexable> results = new HashSet<>();
		for (ECatchment catchment : catchments) {
			switch(catchment.getType()) {
			case BANK:
			case EMPTY:
			case REACH:
			case UNKNOWN:
				results.addAll(catchment.getFlowpaths());
				
				break;
			case WATER_CANAL:
			case WATER_LAKE:
			case WATER_POND:
			case WATER_RIVER:
				results.add(catchment);
				break;
			}
			if(results.size() >= maxResults) break;
		}		
		return results;
	}
		
	public Collection<EFlowpath> getDownstreamEFlowpaths(EFlowpath eFlowpath, int maxResults) {
		if(eFlowpath == null) {
			return Collections.emptyList();
		}
		HashSet<EFlowpath> resultSet = new HashSet<EFlowpath>(maxResults);
		List<EFlowpath> results = new ArrayList<EFlowpath>(maxResults);
		results.add(eFlowpath);
		resultLoop:
			for(int i = 0; i < results.size(); i++) {
				for(EFlowpath downstream: results.get(i).getToNode().getDownFlows()) {
					if(resultSet.add(downstream)) {
						results.add(downstream);
						if(results.size() >= maxResults) {
							break resultLoop;
						}
					}
				}
			}
		return results;
	}

	public List<ECatchment> getUpstreamECatchments(ECatchment eCatchment, int maxResults) {
		if(eCatchment == null) {
			return Collections.emptyList();
		}
		int size = Math.min(ChyfDatastore.MAX_RESULTS, maxResults);
		HashSet<ECatchment> resultSet = new HashSet<ECatchment>(size);
		List<ECatchment> results = new ArrayList<ECatchment>(size);
		results.add(eCatchment);
		resultLoop:
			for(int i = 0; i < results.size(); i++) {
				for(Nexus n: results.get(i).getUpNexuses()) {
					for(EFlowpath f: n.getUpFlows()) {
						if(f.getCatchment() != null) {
							if(resultSet.add(f.getCatchment())) {
								results.add(f.getCatchment());
								if(results.size() >= maxResults) {
									break resultLoop;
								}
							}
						}
					}
					if(n.getType() == NexusType.BANK) {
						if(n.getBankCatchment() != null) {
							if(resultSet.add(n.getBankCatchment())) {
								results.add(n.getBankCatchment());
								if(results.size() >= maxResults) {
									break resultLoop;
								}
							}
						}
					}
				}
			}
		return results;
	}

	public List<ECatchment> getDownstreamECatchments(ECatchment eCatchment, int maxResults) {
		if(eCatchment == null) {
			return Collections.emptyList();
		}
		int size = Math.min(ChyfDatastore.MAX_RESULTS, maxResults);
		HashSet<ECatchment> resultSet = new HashSet<ECatchment>(size);
		ArrayList<ECatchment> results = new ArrayList<ECatchment>(size);
		results.add(eCatchment);
		resultLoop:
			for(int i = 0; i < results.size(); i++) {
				for(Nexus n: results.get(i).getDownNexuses()) {
					for(EFlowpath f: n.getDownFlows()) {
						if(f.getCatchment() != null) {
							if(resultSet.add(f.getCatchment())) {
								results.add(f.getCatchment());
								if(results.size() >= maxResults) {
									break resultLoop;
								}
							}
						}
					}
				}
			}
		return results;
	}

	public DrainageArea getUpstreamDrainageArea(ECatchment eCatchment, boolean removeHoles) {
		return buildDrainageArea(getUpstreamECatchments(eCatchment, Integer.MAX_VALUE), removeHoles);
	}

	public DrainageArea getDownstreamDrainageArea(ECatchment eCatchment, boolean removeHoles) {
		return buildDrainageArea(getDownstreamECatchments(eCatchment, Integer.MAX_VALUE), removeHoles);
	}

	
	public DrainageArea buildDrainageArea(Collection<ECatchment> catchments, boolean removeHoles) {
		List<Geometry> geoms = new ArrayList<Geometry>(catchments.size());
		double area = 0;
		
		StatisticMerger statMerger = new StatisticMerger();
		for(ECatchment c : catchments) {			
			geoms.add(c.getPolygon());
			area += c.getArea();
			statMerger.addCatchment(c);
		}

		Geometry g = UnaryUnionOp.union(geoms);
		DrainageArea da = new DrainageArea(g, area);
		if(removeHoles) {
			da = removeHoles(da, statMerger);		
		}
		
		//assign stats
		HashMap<ECatchment.ECatchmentStat, Double> stats = statMerger.getMergedStats();
		for (ECatchmentStat s : ECatchmentStat.values()) {
			if (stats.containsKey(s)) da.setStat(s, stats.get(s));
		}
		
		return da;
	}

	public DrainageArea removeHoles(DrainageArea current, StatisticMerger statMerger) {
		if(current.getGeometry() instanceof Polygon) {
			//for each interior area we need to find all catchments that overlap it and add that area to the total area
			Polygon p = (Polygon)current.getGeometry();
			
			Double addArea = 0.0;
			for (int i = 0; i < p.getNumInteriorRing(); i ++) {
				Polygon interior = current.getGeometry().getFactory().createPolygon(p.getInteriorRingN(i).getCoordinateSequence());
				List<ECatchment> items = findECatchments(interior);
				for (ECatchment item : items){
					addArea += item.getArea();
					statMerger.addCatchment(item);
				}
			}
			return new DrainageArea(current.getGeometry().getFactory().createPolygon(p.getExteriorRing().getCoordinateSequence()), current.getArea() + addArea);
		}
		
		if(current.getGeometry() instanceof MultiPolygon) {
			MultiPolygon mp = (MultiPolygon)current.getGeometry();
			
			Double addArea = 0.0;
			
			Polygon[] polygons = new Polygon[mp.getNumGeometries()];
			for(int i = 0; i < mp.getNumGeometries(); i++) {
				Polygon p = (Polygon)mp.getGeometryN(i);
				for (int k = 0; k < p.getNumInteriorRing(); k ++) {
					Polygon interior = current.getGeometry().getFactory().createPolygon(p.getInteriorRingN(k).getCoordinateSequence());
					List<ECatchment> items = findECatchments(interior);
					for (ECatchment item : items){
						addArea += item.getArea();
						statMerger.addCatchment(item);
					}
				}
				polygons[i] = current.getGeometry().getFactory().createPolygon(p.getExteriorRing().getCoordinateSequence());
			}
			return new DrainageArea(current.getGeometry().getFactory().createMultiPolygon(polygons), current.getArea() + addArea);
		}
		return current;
	}

}
