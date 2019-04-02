package nrcan.cccmeo.chyf.db;

import java.lang.reflect.Field;

public class Catchment {
	private int id = 0;
	private double area;
	private double elv_min = Double.NaN;
	private double elv_max = Double.NaN;
	private double elv_mean = Double.NaN;
	private double slope_min = Double.NaN;
	private double slope_max = Double.NaN;
	private double slope_mean = Double.NaN;
	private double north_pct = Double.NaN;
	private double south_pct = Double.NaN;
	private double east_pct = Double.NaN;
	private double west_pct = Double.NaN;
	private double flat_pct = Double.NaN;
	private double d2w2d_mean =  Double.NaN;
	private double d2w2d_max =  Double.NaN;
	private String linestring;
	
	Catchment() {

	}
	
	Catchment(int id, double area, String linestring) {
		this.setId(id);
		this.setArea(area);
		this.setLinestring(linestring);
	}
	
	public Object getAttribute(String attrName, Object obj) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		return Catchment.class.getDeclaredField(attrName).get(obj);
	}
	
	public void setAttribute (String attrName, double value) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		Catchment.class.getDeclaredField(attrName).set(this, value);
	}
	
	public Field[] getFieldsName () {
	    return Catchment.class.getDeclaredFields();	
	}

	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}

	public double getArea() {
		return area;
	}

	public void setArea(double area) {
		this.area = area;
	}

	public String getLinestring() {
		return linestring;
	}

	public void setLinestring(String linestring) {
		this.linestring = linestring;
	}

	public double getElv_min() {
		return elv_min;
	}

	public void setElv_min(double elv_min) {
		this.elv_min = elv_min;
	}

	public double getElv_max() {
		return elv_max;
	}

	public void setElv_max(double elv_max) {
		this.elv_max = elv_max;
	}

	public double getElv_mean() {
		return elv_mean;
	}

	public void setElv_mean(double elv_mean) {
		this.elv_mean = elv_mean;
	}

	public double getSlope_min() {
		return slope_min;
	}

	public void setSlope_min(double slope_min) {
		this.slope_min = slope_min;
	}

	public double getSlope_max() {
		return slope_max;
	}

	public void setSlope_max(double slope_max) {
		this.slope_max = slope_max;
	}

	public double getSlope_mean() {
		return slope_mean;
	}

	public void setSlope_mean(double slope_mean) {
		this.slope_mean = slope_mean;
	}

	public double getNorth_pct() {
		return north_pct;
	}

	public void setNorth_pct(double north_pct) {
		this.north_pct = north_pct;
	}

	public double getSouth_pct() {
		return south_pct;
	}

	public void setSouth_pct(double south_pct) {
		this.south_pct = south_pct;
	}

	public double getEast_pct() {
		return east_pct;
	}

	public void setEast_pct(double east_pct) {
		this.east_pct = east_pct;
	}

	public double getWest_pct() {
		return west_pct;
	}

	public void setWest_pct(double west_pct) {
		this.west_pct = west_pct;
	}

	public double getFlat_pct() {
		return flat_pct;
	}

	public void setFlat_pct(double flat_pct) {
		this.flat_pct = flat_pct;
	}

	public double getD2w2d_mean() {
		return d2w2d_mean;
	}

	public void setD2w2d_mean(double d2w2d_mean) {
		this.d2w2d_mean = d2w2d_mean;
	}

	public double getD2w2d_max() {
		return d2w2d_max;
	}

	public void setD2w2d_max(double d2w2d_max) {
		this.d2w2d_max = d2w2d_max;
	}
}
