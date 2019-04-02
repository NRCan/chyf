package nrcan.cccmeo.chyf.db;

public class Waterbody {
	private int definition;
	private double area = Double.NaN;
	private String linestring;

	Waterbody() {
		
	}
	
	Waterbody(int definition, double area, String linestring) {
		this.setDefinition(definition);
		this.setArea(area);
		this.setLinestring(linestring);
	}

	public int getDefinition() {
		return definition;
	}

	public void setDefinition(int definition) {
		this.definition = definition;
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
}
