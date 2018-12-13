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
	private Integer[][] uniqueSubCatchmentRelationship;
	private List<UniqueSubCatchment> uniqueSubCatchments;
	
	private Double[][] minPpDistance;
	private Double[][] maxPpDistance;
	
	private Set<PourpointEngine.OutputType> outputs;
	
	public PourpointOutput(PourpointEngine engine) {
		this.points = engine.getPoints();
		this.pourpointRelationship = engine.getPourpointRelationshipMatrix();
		this.uniqueSubCatchmentRelationship = engine.getUniqueSubCatchmentRelationship();
		this.uniqueSubCatchments = engine.getSortedUniqueSubCatchments();
		minPpDistance = engine.getPourpointMinDistanceMatrix();
		maxPpDistance = engine.getPourpointMaxDistanceMatrix();
		this.outputs = engine.getAvailableOutputs();
	}

	public Set<PourpointEngine.OutputType> getAvailableOutputs(){
		return this.outputs;
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
		return uniqueSubCatchmentRelationship;
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
	
	public Geometry getUniqueCatchment(Pourpoint point) {
		return aggregateAreas(point.getUniqueCatchments());
	}
	
	
	
	public Collection<UniqueSubCatchment> getUniqueSubCatchments(Pourpoint point){
		return point.getUniqueSubCatchments();
	}
	
	public static Geometry aggregateAreas(Set<ECatchment> catchments) {
		List<Geometry> geoms = new ArrayList<Geometry>(catchments.size());
		double area = 0;
		for(ECatchment c : catchments) {
			geoms.add(c.getPolygon());
			area+= c.getArea();
		}
		Geometry g = UnaryUnionOp.union(geoms);
		g.setUserData(area);
		return g;
	}
}
