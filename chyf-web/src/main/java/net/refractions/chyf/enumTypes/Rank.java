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

public enum Rank {
	PRIMARY ("Primary"),
	SECONDARY ("Secondary"),
	UNKNOWN ("Unknown");

	public String rankName;
	
	Rank(String name){
		this.rankName = name;
		
	}
	
	static final Logger logger = LoggerFactory.getLogger(Rank.class.getCanonicalName());

	/**
	 * Takes a string value and returns the corresponding Rank object.
	 * 
	 * @param rank string representation of the Rank
	 * @return the Rank corresponding to the given string representation.
	 */
	public static Rank convert(String rankType) {
		for(Rank rank : values()) {
			if(rank.name().equalsIgnoreCase(rankType) ||
				rank.rankName.equalsIgnoreCase(rankType)) {
				return rank;
			}
		}
		try {
			int index = Integer.parseInt(rankType);
			if (index < Rank.values().length) {
				return Rank.values()[index];
			}
		}catch (Exception ex) {}
		logger.warn("Invalid Rank value: '" + rankType + "' defaulting to 'Unknown'.");
		return UNKNOWN;
	}
}

