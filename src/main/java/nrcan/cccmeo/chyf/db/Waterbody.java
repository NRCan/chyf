package nrcan.cccmeo.chyf.db;

public class Waterbody {
	private int definition;
	private String linestring;

	Waterbody() {
		
	}
	
	Waterbody(int definition, String linestring) {
		this.setDefinition(definition);
		this.setLinestring(linestring);
	}

	public int getDefinition() {
		return definition;
	}

	public void setDefinition(int definition) {
		this.definition = definition;
	}

	public String getLinestring() {
		return linestring;
	}

	public void setLinestring(String linestring) {
		this.linestring = linestring;
	}
}
