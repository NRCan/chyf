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
		logger.warn("Invalid Rank value: '" + rankType + "' defaulting to 'Unknown'.");
		return UNKNOWN;
	}
}

