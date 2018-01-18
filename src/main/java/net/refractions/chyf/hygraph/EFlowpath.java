package net.refractions.chyf.hygraph;

import net.refractions.chyf.enumTypes.FlowpathRank;
import net.refractions.chyf.enumTypes.FlowpathType;
import net.refractions.chyf.indexing.SpatiallyIndexable;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class EFlowpath implements SpatiallyIndexable {
	private final int id;
	private final Nexus fromNode;
	private final Nexus toNode;
	private final double length;
	private final FlowpathType type;
	private final FlowpathRank rank;
	private final String name;
	private int strahlerOrder;
	private int hortonOrder;
	private int hackOrder;
	private final ECatchment catchment;
	private final LineString lineString;
	
	public EFlowpath(int id, Nexus fromNode, Nexus toNode, double length, FlowpathType type, FlowpathRank rank, String name, 
			int strahlerOrder, int hortonOrder, int hackOrder, ECatchment catchment, LineString lineString) {
		this.id = id;
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.length = length;
		this.type = type;
		this.rank = rank;
		this.name = name;
		this.setStrahlerOrder(strahlerOrder);
		this.setHortonOrder(hortonOrder);
		this.setHackOrder(hackOrder);
		this.catchment = catchment;
		this.lineString = lineString;
	}

	public int getId() {
		return id;
	}

	public Nexus getFromNode() {
		return fromNode;
	}

	public Nexus getToNode() {
		return toNode;
	}

	public LineString getLineString() {
		return lineString;
	}

	public double getLength() {
		return length;
	}
	
	public FlowpathType getType() {
		return type;
	}

	public FlowpathRank getRank() {
		return rank;
	}
	
	public String getName() {
		return name;
	}

	public int getStrahlerOrder() {
		return strahlerOrder;
	}

	public void setStrahlerOrder(int strahlerOrder) {
		this.strahlerOrder = strahlerOrder;
	}

	public int getHortonOrder() {
		return hortonOrder;
	}

	public void setHortonOrder(int hortonOrder) {
		this.hortonOrder = hortonOrder;
	}

	public int getHackOrder() {
		return hackOrder;
	}

	public void setHackOrder(int hackOrder) {
		this.hackOrder = hackOrder;
	}

	public ECatchment getCatchment() {
		return catchment;
	}

	public Nexus getOtherNode(final Nexus node) {
		if(node == toNode) {
			return fromNode;
		}
		if(node == fromNode) {
			return toNode;
		}
		return null;
	}

	@Override
	public Envelope getEnvelope() {
		return lineString.getEnvelopeInternal();
	}

	@Override
	public double distance(Point p) {
		return lineString.distance(p);
	}

}
