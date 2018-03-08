package net.refractions.chyf.hygraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.enumTypes.CatchmentType;
import net.refractions.chyf.enumTypes.NexusType;
import net.refractions.chyf.indexing.ECatchmentContainsPointFilter;
import net.refractions.chyf.indexing.Filter;
import net.refractions.chyf.indexing.RTree;
import net.refractions.chyf.indexing.SpatiallyIndexable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.union.UnaryUnionOp;

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

	public List<EFlowpath> findEFlowpaths(Point p, int maxResults, Integer maxDistance, Filter<EFlowpath> f) {
		return eFlowpathIndex.search(p, maxResults, maxDistance, f);
	}

	public ECatchment getECatchment(int ecatchmentId) {
		if(ecatchmentId > 0 && ecatchmentId <= eCatchments.length) {
			return eCatchments[ecatchmentId-1];
		}
		return null;	
	}

	public ECatchment getECatchment(Point point) {
		List<ECatchment> eCatchments = findECatchments(point, 1, 0, 
				new ECatchmentContainsPointFilter(point));
		if(eCatchments.size() > 0) {
			return eCatchments.get(0);
		}
		return null;
	}
	
	public List<ECatchment> findECatchments(Point p, int maxResults, Integer maxDistance, Filter<ECatchment> f) {
		return eCatchmentIndex.search(p, maxResults, maxDistance, f);
	}

	public List<Nexus> findNexuses(Point p, int maxResults, Integer maxDistance, Filter<Nexus> f) {
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

	public Object getDownstreamEFlowpaths(EFlowpath eFlowpath, int maxResults) {
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

	private DrainageArea buildDrainageArea(List<ECatchment> catchments, boolean removeHoles) {
		List<Geometry> geoms = new ArrayList<Geometry>(catchments.size());
		for(ECatchment c : catchments) {
			geoms.add(c.getPolygon());
		}
		Geometry g = UnaryUnionOp.union(geoms);
		if(removeHoles) {
			g = removeHoles(g);
		}
		return new DrainageArea(g);
	}

	private Geometry removeHoles(Geometry g) {
		if(g instanceof Polygon) {
			return g.getFactory().createPolygon(((Polygon)g).getExteriorRing().getCoordinateSequence());
		}
		if(g instanceof MultiPolygon) {
			Polygon[] polygons = new Polygon[g.getNumGeometries()];
			for(int i = 0; i < g.getNumGeometries(); i++) {
				polygons[i] = (Polygon)removeHoles(g.getGeometryN(i));
			}
			return g.getFactory().createMultiPolygon(polygons);
		}
		return g;
	}

}
