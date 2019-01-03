package net.refractions.chyf.pourpoint;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.refractions.chyf.hygraph.DrainageArea;
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
	private Integer[][] nocr;
	private Integer[][] tccr;
	private Integer[][] pcc;
	private List<UniqueSubCatchment> tcc;
	
	private Double[][] minPpDistance;
	private Double[][] maxPpDistance;
	
	private Set<PourpointEngine.OutputType> outputs;
	
	private boolean removeHoles;
	
	public PourpointOutput(PourpointEngine engine) {
		this.points = engine.getPoints();
		this.removeHoles = engine.getRemoveHoles();
		this.nocr = engine.getNonOverlappingCoverageRelationship();
		this.tccr = engine.getTraversalCompliantCoverageRelationship();
		this.tcc = engine.getSortedTraveralCompliantCoverages();
		this.pcc = engine.getCatchmentContainment();
		minPpDistance = engine.getProjectedPourpointMinDistanceMatrix();
		maxPpDistance = engine.getProjectedPourpointMaxDistanceMatrix();
		this.outputs = engine.getAvailableOutputs();
	}

	public Set<PourpointEngine.OutputType> getAvailableOutputs(){
		return this.outputs;
	}
	public List<Pourpoint> getPoints() {
		return points;
	}

	public Double[][] getProjectedPourpointMinDistanceMatrix(){
		return minPpDistance;
	}
	public Double[][] getProjectedPourpointMaxDistanceMatrix(){
		return maxPpDistance;
	}
	
	public Integer[][] getCatchmentContainment(){
		return this.pcc;
	}
	
	public Integer[][] getNonOverlappingCatchmentRelationship() {
		return nocr;
	}

	public Integer[][] getTraversalCompliantCatchmentRelationship() {
		return tccr;
	}

	public List<UniqueSubCatchment> getTraversalCompliantCatchments() {
		return tcc;
	}
	
	public DrainageArea getCatchment(Pourpoint point) {
		return point.getCatchmentDrainageArea(removeHoles);
		
	}
	
	public DrainageArea getNonOverlappingCatchments(Pourpoint point) {
		return HyGraph.buildDrainageArea(point.getUniqueCatchments(), false);
	}
	
	
	
	public Collection<UniqueSubCatchment> getTraversalCompliantCatchments(Pourpoint point){
		return point.getTraversalCompliantCatchments();
	}
	

}
