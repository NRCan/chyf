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

public enum NexusType {
	HEADWATER("Headwater"),
	TERMINAL_ISOLATED("Terminal Isolated"),
	TERMINAL_BOUNDARY("Terminal Boundary"),
	FLOWPATH("Flowpath"),
	WATER("Water"),
	BANK("Bank"),
	INFERRED("Inferred"),
	UNKNOWN("Unknown");
	
	static final Logger logger = LoggerFactory.getLogger(FlowpathType.class.getCanonicalName());

	private String label;
	
	private NexusType(String label) {
		this.label = label;
	}
	
	/**
	 * Takes a string value and returns the corresponding NexusType object.
	 * 
	 * @param nexusType string representation of the NexusType
	 * @return the NexusType corresponding to the given string representation.
	 */
	public static NexusType convert(String nexusType) {
		for(NexusType nt : values()) {
			if(nt.label.equalsIgnoreCase(nexusType)) {
				return nt;
			}
		}
		logger.warn("Invalid NexusType value: '" + nexusType + "' defaulting to 'Unknown'.");
		return UNKNOWN;
	}
	
	/**
	 * @return the string representation of this NexusType object
	 */
	@Override
	public String toString() {
		return label;
	}

	/**
	 * 
	 * @return true if this is a terminal node, irregardless
	 * of the terminal type
	 */
	public boolean isTerminal() {
		return this == TERMINAL_ISOLATED || this == NexusType.TERMINAL_BOUNDARY;
	}
}
