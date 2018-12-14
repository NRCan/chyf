package net.refractions.chyf.pourpoint;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.refractions.chyf.hygraph.DrainageArea;
import net.refractions.chyf.hygraph.ECatchment;
import net.refractions.chyf.hygraph.HyGraph;

/**
 * Class containing all the requested outputs
 * from the pourpoint engine.
 * 
 * @author Emily
 *
 */
public class PourpointOutput {

	private List<Pourpoint> points;
	private Integer[][] pourpointRelationship;
	private Integer[][] uniqueSubCatchmentRelationship;
	private List<UniqueSubCatchment> uniqueSubCatchments;
	
	private Double[][] minPpDistance;
	private Double[][] maxPpDistance;
	
	private Set<PourpointEngine.OutputType> outputs;
	
	private boolean removeHoles;
	
	public PourpointOutput(PourpointEngine engine) {
		this.points = engine.getPoints();
		this.removeHoles = engine.getRemoveHoles();
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
	
	public DrainageArea getCatchment(Pourpoint point) {
		Set<ECatchment> items = new HashSet<>();
		items.addAll(point.getSharedCatchments());
		items.addAll(point.getUniqueCatchments());
		return HyGraph.buildDrainageArea(items, removeHoles);
		
	}
	
	public DrainageArea getUniqueCatchment(Pourpoint point) {
		return HyGraph.buildDrainageArea(point.getUniqueCatchments(), false);
	}
	
	
	
	public Collection<UniqueSubCatchment> getUniqueSubCatchments(Pourpoint point){
		return point.getUniqueSubCatchments();
	}
	

}
