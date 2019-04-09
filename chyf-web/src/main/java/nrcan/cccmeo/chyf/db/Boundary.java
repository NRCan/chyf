package nrcan.cccmeo.chyf.db;

public class Boundary {

	private String linestring;

	Boundary() {
		
	}
	
	Boundary(String linestring) {
		this.setLinestring(linestring);
	}

	public String getLinestring() {
		return linestring;
	}

	public void setLinestring(String linestring) {
		this.linestring = linestring;
	}
}
