package net.refractions.chyf.pourpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.union.UnaryUnionOp;

import net.refractions.chyf.hygraph.ECatchment;

public class PourpointOutput {

	private List<Pourpoint> points;
	private Integer[][] pourpointRelationship;
	private Integer[][] uniqueSubcCtchmentRelationship;
	private List<UniqueSubCatchment> uniqueSubCatchments;
	
	private Double[][] minPpDistance;
	private Double[][] maxPpDistance;
	
	public PourpointOutput(PourpointEngine engine) {
		this.points = engine.getPoints();
		this.pourpointRelationship = engine.getPourpointRelationshipMatrix();
		this.uniqueSubcCtchmentRelationship = engine.getPourpointCatchmentRelationship();
		this.uniqueSubCatchments = engine.getSortedUniqueSubCatchments();
		minPpDistance = engine.getPourpointMinDistanceMatrix();
		maxPpDistance = engine.getPourpointMaxDistanceMatrix();
	}

	public List<Pourpoint> getPoints() {
		return points;
	}

	public Double[][] getPourpointMinDistanceMatrix(){
		return minPpDistance;
	}
	public Double[][] getPourpointMaxDistanceMatrix(){
		return maxPpDistance;
	}
	
	
	public Integer[][] getPourpointRelationship() {
		return pourpointRelationship;
	}

	public Integer[][] getPourpointCatchmentRelationship() {
		return uniqueSubcCtchmentRelationship;
	}

	public List<UniqueSubCatchment> getUniqueSubCatchments() {
		return uniqueSubCatchments;
	}
	
	public Geometry getCatchment(Pourpoint point) {
		Set<ECatchment> items = new HashSet<>();
		items.addAll(point.getSharedCatchments());
		items.addAll(point.getUniqueCatchments());
		return aggregateAreas(items);
	}
	
	public Geometry getCombinedUniqueCatchment(Pourpoint point) {
		Set<ECatchment> items = new HashSet<>();
		items.addAll(point.getUniqueCatchments());
		return aggregateAreas(items);
	}
	
	public Set<ECatchment> getUniqueCatchments(Pourpoint point){
		return point.getUniqueCatchments();
	}
	
	public Collection<UniqueSubCatchment> getSubCombinedUniqueCatchments(Pourpoint point){
		return point.getUniqueCombinedCatchments();
	}
	
	public static Geometry aggregateAreas(Set<ECatchment> catchments) {
		List<Geometry> geoms = new ArrayList<Geometry>(catchments.size());
		for(ECatchment c : catchments) {
			geoms.add(c.getPolygon());
		}
		Geometry g = UnaryUnionOp.union(geoms);
		return g;
	}
}
