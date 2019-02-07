package nrcan.cccmeo.chyf.db;

public class Catchment {
	private String linestring;
	
	Catchment() {
		
	}
	
	Catchment(String linestring) {
		this.setLinestring(linestring);
	}

	public String getLinestring() {
		return linestring;
	}

	public void setLinestring(String linestring) {
		this.linestring = linestring;
	}
}
