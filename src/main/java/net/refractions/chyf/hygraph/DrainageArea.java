package net.refractions.chyf.hygraph;

import org.locationtech.jts.geom.Geometry;

public class DrainageArea {
	
	private final double area;
	private final Geometry g;
	private String id;
	
	public DrainageArea(Geometry g, double area) {
		this(g, area, null);
	}
	
	public DrainageArea(Geometry g, double area, String id) {
		this.area = area;
		this.g = g;
		this.id = id;
	}

	public double getArea() {
		return area;
	}

	public Geometry getGeometry() {
		return g;
	}
	
	public String getId() {
		return this.id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
}
