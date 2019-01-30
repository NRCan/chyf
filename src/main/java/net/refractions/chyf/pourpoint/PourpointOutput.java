package net.refractions.chyf.pourpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import net.refractions.chyf.hygraph.DrainageArea;
import net.refractions.chyf.pourpoint.PourpointEngine.OutputType;

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
	private List<DrainageArea> tcc;
	
	private Double[][] minPpDistance;
	private Double[][] maxPpDistance;
	private Double[][] primaryPpDistance;
	
	private Set<PourpointEngine.OutputType> outputs;
	private Set<DrainageArea> interiorCatchments;
	private String prt = "";
	
	
	private HashMap<Pourpoint, DrainageArea> catchments;
	private HashMap<Pourpoint, DrainageArea> partitionedcatchments;
	
	public PourpointOutput(PourpointEngine engine) {
		this.points = engine.getPoints();
		this.pcr = engine.getPartitionedCatchmentRelationship();
		this.tccr = engine.getTraversalCompliantCoverageRelationship();
		if (engine.getSortedTraveralCompliantCoverages() != null) {
			tcc = new ArrayList<>();
			for (UniqueSubCatchment c : engine.getSortedTraveralCompliantCoverages()) {
				DrainageArea da = c.getDrainageArea(engine.getGraph());
				da.setId(c.getId());
				tcc.add(da);
			}
		}
		this.ccr = engine.getCatchmentContainment();
		this.interiorCatchments = engine.getInteriorCatchments();
		
		minPpDistance = engine.getProjectedPourpointMinDistanceMatrix();
		maxPpDistance = engine.getProjectedPourpointMaxDistanceMatrix();
		this.primaryPpDistance = engine.getProjectedPourpointPrimaryDistanceMatrix();
		this.outputs = engine.getAvailableOutputs();
		this.prt = engine.getPointRelationshipTree();
		
		if (engine.getAvailableOutputs().contains(OutputType.CATCHMENTS)) {
			catchments = new HashMap<>();
			for (Pourpoint p : points) {
				catchments.put(p, p.getCatchmentDrainageArea(engine.getGraph(), engine.getRemoveHoles()));
			}
		}
		if (engine.getAvailableOutputs().contains(OutputType.PARTITIONED_CATCHMENTS)) {
			partitionedcatchments = new HashMap<>();
			for (Pourpoint p : points) {
				partitionedcatchments.put(p, engine.getGraph().buildDrainageArea(p.getUniqueCatchments(), false));
			}
		}
		
	}

	public String getPointRelationshipTree() {
		return this.prt;
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
	
	public Double[][] getProjectedPourpointPrimaryDistanceMatrix(){
		return primaryPpDistance;
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

	public List<DrainageArea> getTraversalCompliantCatchments() {
		return tcc;
	}
	
	public DrainageArea getCatchment(Pourpoint point) {
		return catchments.get(point);
	}
	public DrainageArea getPartitionedCatchment(Pourpoint point) {
		return partitionedcatchments.get(point);
	}


}
