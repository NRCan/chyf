/*
 * Copyright 2019 Government of Canada
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package net.refractions.chyf.enumTypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum CatchmentType {
	REACH("Reach", null),
	BANK("Bank", null),
	WATER_LAKE("Water", "Lake"),
	WATER_POND("Water", "Pond"),
	WATER_RIVER("Water", "River"),
	WATER_CANAL("Water", "Canal"),
	EMPTY("EmptyCatchment", null),
	UNKNOWN("Unknown", null);
	
	static final Logger logger = LoggerFactory.getLogger(CatchmentType.class.getCanonicalName());

	private String typeLabel;
	private String subTypeLabel;
	
	private CatchmentType(String typeLabel, String subTypeLabel) {
		this.typeLabel = typeLabel;
		this.subTypeLabel = subTypeLabel;
	}
	
	public boolean isWaterbody() {
		return this == CatchmentType.WATER_LAKE || 
				this == CatchmentType.WATER_POND || 
				this == CatchmentType.WATER_RIVER || 
				this == CatchmentType.WATER_CANAL;
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
