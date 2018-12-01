package net.refractions.chyf.pourpoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

	private HashMap<ECatchment, Set<ECatchment>> catchmentRelationship;
	
	private boolean removeHoles = false;
	
	public static int TEST_DATA_SRID = 4617;
	
	public PourpointEngine(List<Pourpoint> points, HyGraph hygraph) {
		this.points = Collections.unmodifiableList(points);
		this.hygraph = hygraph;
	}
	
	public PourpointOutput compute() {
		//compute downstream flowpaths for pourpoints
		points.forEach(p->p.findDownstreamFlowpaths(hygraph));
		
		//compute pourpoint relationship
		computeUpstreamDownstreamRelationship();
		
		//catchments for pourpoints
		computeCatchments();
		
		//catchment relationships
		computeCatchmentRelationship();
		
		return new PourpointOutput(this);
		
//		computeUpstreamCatchments();
//		System.out.println("---Upstream---");
//		for (Entry<Pourpoint, List<ECatchment>> r : upstreamCatchments.entrySet()) {
//			System.out.println(GeotoolsGeometryReprojector.reproject(r.getKey().getPoint(), TEST_DATA_SRID).toText());
//			DrainageArea da = aggregateAreas(r.getValue());
//			System.out.println(GeotoolsGeometryReprojector.reproject(da.getGeometry(),TEST_DATA_SRID ).toText());
//		}
//		
//		computeUniqueCatchments();
//		System.out.println("---Unique---");
//		for (Entry<Pourpoint, List<ECatchment>> r : uniqueCatchments.entrySet()) {
//			System.out.println(GeotoolsGeometryReprojector.reproject(r.getKey().getPoint(), TEST_DATA_SRID).toText());
//			DrainageArea da = aggregateAreas(r.getValue());
//			System.out.println(GeotoolsGeometryReprojector.reproject(da.getGeometry(),TEST_DATA_SRID ).toText());
//		}
	}
	
	public List<Pourpoint> getPoints(){
		return this.points;
	}
	
	public List<ECatchment> getSortedCatchments(){
		Set<ECatchment> allCatchments = new HashSet<>();
		for (ECatchment c : catchmentRelationship.keySet()) allCatchments.add(c);
		for (Set<ECatchment> c : catchmentRelationship.values()) allCatchments.addAll(c);
		
		List<ECatchment> ordered = new ArrayList<>();
		ordered.addAll(allCatchments);
		ordered.sort((a,b)->Integer.compare(a.getId(), b.getId()));
		return ordered;
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
	
	/**
	 * results are ordered by catchment id
	 * @return
	 */
	public Integer[][] getCatchmentRelationshipMatrix(){
		List<ECatchment> ordered = getSortedCatchments();
		Integer[][] values = new Integer[ordered.size()][ordered.size()];
		for (int i = 0 ; i < ordered.size(); i ++) {
			for (int j = 0 ; j < ordered.size(); j ++) {
				if (i == j) { values[i][j] = null; continue; }
				
				ECatchment pi = ordered.get(i);
				ECatchment pj = ordered.get(j);
				
				if (catchmentRelationship.containsKey(pi) && catchmentRelationship.get(pi).contains(pj)) {
					values[i][j] = 1;
				}else if (catchmentRelationship.containsKey(pj) && catchmentRelationship.get(pj).contains(pi)) {
					values[i][j] = -1;
				}else {
					values[i][j] = null;
				}
			}
		}
		return values;
	}
	
	
	private void computeCatchmentRelationship() {
		catchmentRelationship = new HashMap<>();
		
		//find most downstream catchments 
		List<ECatchment> edownstream = new ArrayList<>();
		for (Pourpoint p : points) {
			if (p.getDownstreamPourpoints().isEmpty()) {
				p.getDownstreamFlowpaths().forEach(fp->edownstream.add(fp.getCatchment()));
			}
		}
		//walk up these catchments tracking relationships
		for (ECatchment root : edownstream) {
			processCatchment(root, catchmentRelationship);
		}
	}
	
	//walking upstream
	private void processCatchment(ECatchment root, HashMap<ECatchment, Set<ECatchment>> down) {
		for (Nexus upN : root.getUpNexuses() ) {
			List<ECatchment> process = new ArrayList<>();
			upN.getUpFlows().forEach(f->{
				process.add(f.getCatchment());
			});
			if (upN.getBankCatchment() != null) {
				process.add(upN.getBankCatchment());
			}
			//cat is upstream of root
			
			process.forEach(up ->{
				Set<ECatchment> c = down.get(up);
				if (c == null) {
					c = new HashSet<>();
					down.put(up,  c);
				}
				c.add(root);
				if (down.get(root) != null) c.addAll(down.get(root));
			});
			process.forEach(p->processCatchment(p, down));

		}
	}
	
	
	private void computeUpstreamDownstreamRelationship() {

		HashMap<EFlowpath, Set<Pourpoint>> flowpathToUpPp = new HashMap<>();
		for (Pourpoint pp : points) {
			//walk downstream until we reach the network terminus
			for (EFlowpath edge : pp.getDownstreamFlowpaths()) {
				if (flowpathToUpPp.containsKey(edge)) continue;
				processEdge(edge, Collections.singleton(pp), flowpathToUpPp);
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
	
	private void processEdge(EFlowpath path, Set<Pourpoint> upstream, HashMap<EFlowpath, Set<Pourpoint>> flowpathToUpPp) {
		Set<Pourpoint> pointsOnEdge = new HashSet<>();
		for (Pourpoint pp : points) {
			for (EFlowpath dpath : pp.getDownstreamFlowpaths()) {
				if (dpath.equals(path)) {
					pointsOnEdge.add(pp);
					break;
				}
				
			}
		}
		pointsOnEdge.addAll(upstream);
		Set<Pourpoint> temp = flowpathToUpPp.get(path);
		if (temp == null) {
			temp = new HashSet<>();
			flowpathToUpPp.put(path, temp);
		}
		temp.addAll(pointsOnEdge);

		for (EFlowpath downstream: path.getToNode().getDownFlows()) {
			processEdge(downstream, pointsOnEdge, flowpathToUpPp);
		}
	}
	
	
	private void computeCatchments() {
		List<Pourpoint> toProcess = new ArrayList<>();
		
		for (Pourpoint pp : points) {
			if (pp.getUpstreamPourpoints().isEmpty()) toProcess.add(pp);
		}
		
		HashMap<EFlowpath, Set<ECatchment>> catchments = new HashMap<>();
		
		Set<Pourpoint> processed = new HashSet<>();
		while(!toProcess.isEmpty()) {
			Pourpoint item = toProcess.remove(0);
			processed.add(item);
			for (EFlowpath path : item.getDownstreamFlowpaths()) {
				List<ECatchment>[] results = computeUpstreamCatchments(path, catchments);
				
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
		
		
	}
	
	private List<ECatchment>[] computeUpstreamCatchments(EFlowpath root, HashMap<EFlowpath, Set<ECatchment>> catchments){
		
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
	
	
//	private void computeUpstreamCatchments() {
//		upstreamCatchments = new HashMap<>();
//		
//		for (Pourpoint p : points) {
//			List<ECatchment> temp = new ArrayList<>();
//			for (EFlowpath path : p.getDownstreamFlowpaths()) {
//				temp.addAll( hygraph.getUpstreamECatchments(path.getCatchment(), ChyfDatastore.MAX_RESULTS));
//			}
//			upstreamCatchments.put(p,  temp);
//			
//		}
//	}
//	
//	private void computeUniqueCatchments() {
//		HashMap<Pourpoint, List<ECatchment>> uniqueCatchments = new HashMap<>();
//		HashMap<Pourpoint, List<ECatchment>> upstreamCatchments = new HashMap<>();
//		
//		TreeMap<Integer, Pourpoint> catchmentsize = new TreeMap<>();
//		
//		for (Entry<Pourpoint, List<ECatchment>> map : upstreamCatchments.entrySet()) {
//			catchmentsize.put(map.getValue().size(),map.getKey());
//		}
//		
//		List<ECatchment> processed = new ArrayList<>();
//		while(!catchmentsize.isEmpty()) {
//			Pourpoint pourpoint = catchmentsize.remove(catchmentsize.firstKey());
//			
//			List<ECatchment> catchments = upstreamCatchments.get(pourpoint);
//			catchments.removeAll(processed);
//			uniqueCatchments.put(pourpoint, catchments);
//			processed.addAll(catchments);
//			
//		}
//		
//		
//		
//	}
//	
	
	private DrainageArea aggregateAreas(Set<ECatchment> catchments) {
		List<Geometry> geoms = new ArrayList<Geometry>(catchments.size());
		for(ECatchment c : catchments) {
			geoms.add(c.getPolygon());
		}
		Geometry g = UnaryUnionOp.union(geoms);
//		if(removeHoles) {
//			g = removeHoles(g);
//		}
		return new DrainageArea(g);
	}
}
