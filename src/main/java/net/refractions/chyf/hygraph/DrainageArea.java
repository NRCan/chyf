package net.refractions.chyf.hygraph;

import com.vividsolutions.jts.geom.Geometry;

public class DrainageArea {
	private final double area;
	private final Geometry g;

	public DrainageArea(Geometry g) {
		this.area = g.getArea();
		this.g = g;
	}

	public double getArea() {
		return area;
	}

	public Geometry getGeometry() {
		return g;
	}
}
