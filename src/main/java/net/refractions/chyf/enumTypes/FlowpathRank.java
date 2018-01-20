package net.refractions.chyf.enumTypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum FlowpathRank {
	PRIMARY("Primary"),
	SECONDARY("Secondary"),
	UNKNOWN("Unknown");
	
	static final Logger logger = LoggerFactory.getLogger(FlowpathRank.class.getCanonicalName());

	private String label;
	
	private FlowpathRank(String label) {
		this.label = label;
	}
	
	/**
	 * Takes a string value and returns the corresponding FlowpathRank object.
	 * 
	 * @param flowpathRank string representation of the FlowpathRank
	 * @return the FlowpathRank corresponding to the given string representation.
	 */
	public static FlowpathRank convert(String flowpathRank) {
		for(FlowpathRank fr : values()) {
			if(fr.label.equalsIgnoreCase(flowpathRank)) {
				return fr;
			}
		}
		logger.warn("Invalid FlowpathRank value: '" + flowpathRank + "' defaulting to 'Unknown'.");
		return UNKNOWN;
	}
	
	/**
	 * @return the string representation of this FlowpathRank object
	 */
	@Override
	public String toString() {
		return label;
	}
	
}
