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
