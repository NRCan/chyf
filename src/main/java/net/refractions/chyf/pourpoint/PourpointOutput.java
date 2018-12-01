package net.refractions.chyf.pourpoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.union.UnaryUnionOp;

import net.refractions.chyf.hygraph.ECatchment;

public class PourpointOutput {

	private List<Pourpoint> points;
	private Integer[][] pourpointRelationship;
	private Integer[][] catchmentRelationship;
	private List<ECatchment> allCatchments;
	
	public PourpointOutput(PourpointEngine engine) {
		this.points = engine.getPoints();
		this.pourpointRelationship = engine.getPourpointRelationshipMatrix();
		this.catchmentRelationship = engine.getCatchmentRelationshipMatrix();
		this.allCatchments = engine.getSortedCatchments();
	}

	public List<Pourpoint> getPoints() {
		return points;
	}

	public Integer[][] getPourpointRelationship() {
		return pourpointRelationship;
	}

	public Integer[][] getCatchmentRelationship() {
		return catchmentRelationship;
	}

	public List<ECatchment> getAllCatchments() {
		return allCatchments;
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
	
	private Geometry aggregateAreas(Set<ECatchment> catchments) {
		List<Geometry> geoms = new ArrayList<Geometry>(catchments.size());
		for(ECatchment c : catchments) {
			geoms.add(c.getPolygon());
		}
		Geometry g = UnaryUnionOp.union(geoms);
		return g;
	}
}
