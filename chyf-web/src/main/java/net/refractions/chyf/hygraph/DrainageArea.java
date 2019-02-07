package net.refractions.chyf.hygraph;

import java.util.HashMap;

import org.locationtech.jts.geom.Geometry;

public class DrainageArea {
	
	private final double area;
	private final Geometry g;
	private String id;
	
	private HashMap<ECatchment.ECatchmentStat, Double> areaStats;
	
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
	
	public boolean hasStats() {
		return areaStats != null && areaStats.size() > 0;
	}
	
	public void setStat(ECatchment.ECatchmentStat stat, Double value) {
		if (areaStats == null) areaStats = new HashMap<>();
		areaStats.put(stat, value);
	}
	
	public Double getStat(ECatchment.ECatchmentStat stat) {
		if (areaStats == null) return null;
		return areaStats.get(stat);
	}
}
