package net.refractions.chyf.enumTypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum CatchmentType {
	REACH_CATCHMENT ("ReachCatchment", null),
	BANK_CATCHMENT ("BankCatchment", null),
	WATER_CATCHMENT_LAKE ("WaterCatchment", "Lake"),
	WATER_CATCHMENT_POND ("WaterCatchment", "Pond"),
	WATER_CATCHMENT_RIVER ("WaterCatchment", "River"),
	WATER_CATCHMENT_CANAL ("WaterCatchment", "Canal"),
	EMPTY_CATCHMENT ("EmptyCatchment", null),
	UNKNOWN("Unknown", null);
	
	static final Logger logger = LoggerFactory.getLogger(CatchmentType.class.getCanonicalName());

	private String typeLabel;
	private String subTypeLabel;
	
	private CatchmentType(String typeLabel, String subTypeLabel) {
		this.typeLabel = typeLabel;
		this.subTypeLabel = subTypeLabel;
	}
	
	/**
	 * Takes a string value and returns the corresponding CatchmentType object.
	 * 
	 * @param catchmentType string representation of the CatchmentType
	 * @return the CatchmentType corresponding to the given string representation.
	 */
	public static CatchmentType convert(String catchmentType) {
		for(CatchmentType ft : values()) {
			if(ft.typeLabel.equalsIgnoreCase(catchmentType)) {
				return ft;
			}
		}
		logger.warn("Invalid CatchmentType value: '" + catchmentType + "' defaulting to 'Unknown'.");
		return UNKNOWN;
	}
	
	/**
	 * @return the string representation of this CatchmentType object
	 */
	@Override
	public String toString() {
		return typeLabel;
	}

	public String getSubType() {
		return subTypeLabel;
	}

}
