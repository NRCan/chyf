package net.refractions.chyf.enumTypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum FlowpathType {
	OBSERVED("Observed"),
	CONSTRUCTED("Constructed"),
	INFERRED("Inferred"),
	BANK("Bank"),
	UNKNOWN("Unknown");
	
	static final Logger logger = LoggerFactory.getLogger(FlowpathType.class.getCanonicalName());

	private String label;
	
	private FlowpathType(String label) {
		this.label = label;
	}
	
	/**
	 * Takes a string value and returns the corresponding FlowpathType object.
	 * 
	 * @param flowpathType string representation of the FlowpathType
	 * @return the FlowpathType corresponding to the given string representation.
	 */
	public static FlowpathType convert(String flowpathType) {
		for(FlowpathType ft : values()) {
			if(ft.label.equalsIgnoreCase(flowpathType)) {
				return ft;
			}
		}
		logger.warn("Invalid FlowpathType value: '" + flowpathType + "' defaulting to 'Unknown'.");
		return UNKNOWN;
	}
	
	/**
	 * @return the string representation of this FlowpathType object
	 */
	@Override
	public String toString() {
		return label;
	}
	
}
