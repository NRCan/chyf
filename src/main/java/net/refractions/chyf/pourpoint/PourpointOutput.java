package net.refractions.chyf.pourpoint;

import java.util.ArrayList;
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
	private Integer[][] pcr;
	private Integer[][] tccr;
	private Integer[][] ccr;
	private List<UniqueSubCatchment> tcc;
	
	private Double[][] minPpDistance;
	private Double[][] maxPpDistance;
	
	private Set<PourpointEngine.OutputType> outputs;
	private Set<DrainageArea> interiorCatchments;
	
	private boolean removeHoles;
	
	public PourpointOutput(PourpointEngine engine) {
		this.points = engine.getPoints();
		this.removeHoles = engine.getRemoveHoles();
		this.pcr = engine.getPartitionedCatchmentRelationship();
		this.tccr = engine.getTraversalCompliantCoverageRelationship();
		this.tcc = engine.getSortedTraveralCompliantCoverages();
		this.ccr = engine.getCatchmentContainment();
		this.interiorCatchments = engine.getInteriorCatchments();
		
		minPpDistance = engine.getProjectedPourpointMinDistanceMatrix();
		maxPpDistance = engine.getProjectedPourpointMaxDistanceMatrix();
		this.outputs = engine.getAvailableOutputs();
		
	}

	public Set<DrainageArea> getInteriorCatchments(){
		return this.interiorCatchments;
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
		return this.ccr;
	}
	
	public Integer[][] getPartitionedCatchmentRelationship() {
		return pcr;
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
	
	public DrainageArea getPartitionedCatchments(Pourpoint point) {
		return HyGraph.buildDrainageArea(point.getUniqueCatchments(), false);
	}
	
	public Collection<UniqueSubCatchment> getTraversalCompliantCatchments(Pourpoint point){
		return point.getTraversalCompliantCatchments();
	}
	

}
