package net.refractions.chyf.rest;

public class FilterParameters extends SharedParameters {
	
	private String property;
	private String value;
	private String predicate;
	
	public void setProperty(String property) {
		this.property = property;
	}

	public String getProperty() {
		return property;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setPredicate(String predicate) {
		this.predicate = predicate;
	} 

	public String getPredicate() {
		return predicate;
	}

}
