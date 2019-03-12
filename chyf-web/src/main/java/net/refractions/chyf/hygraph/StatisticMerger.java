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
package net.refractions.chyf.hygraph;

import java.util.HashMap;

import net.refractions.chyf.hygraph.ECatchment.ECatchmentStat;

/**
 * Class for merging catchment statistics (aspect, elevation, slope etc)
 * 
 * @author Emily
 *
 */
public class StatisticMerger {

	private ECatchmentStat[] singleValues = new ECatchmentStat[]{ECatchmentStat.MIN_ELEVATION, ECatchmentStat.MAX_ELEVATION, ECatchmentStat.MIN_SLOPE, ECatchmentStat.MAX_SLOPE, ECatchmentStat.MAX_D2W_2D};
	private ECatchmentStat[] awaValues = new ECatchmentStat[]{ECatchmentStat.MEAN_ELEVATION, ECatchmentStat.MEAN_SLOPE, ECatchmentStat.ASPECT_EAST_PCT,  ECatchmentStat.ASPECT_NORTH_PCT, ECatchmentStat.ASPECT_WEST_PCT, ECatchmentStat.ASPECT_SOUTH_PCT, ECatchmentStat.ASPECT_FLAT_PCT, ECatchmentStat.MEAN_D2W_2D};

	private HashMap<ECatchment.ECatchmentStat, Double> stats = new HashMap<>();
	private HashMap<ECatchment.ECatchmentStat, Double> areaWAverages = new HashMap<>();
	
	private double totalArea = 0;
	
	public StatisticMerger() {
		
	}
	
	public HashMap<ECatchment.ECatchmentStat, Double> getMergedStats(){
		for (ECatchmentStat a : awaValues) {
			Double value = areaWAverages.get(a);
			if (value != null) stats.put(a, value / totalArea);
		}
		return stats;
	}
	
	public void addCatchment(ECatchment eCatchment) {
		for (ECatchmentStat s : singleValues) {
			updateStat(stats, s, s.getValue(eCatchment));
		}
		
		for (ECatchmentStat s : awaValues) {
			if (Double.isNaN(s.getValue(eCatchment))) continue;
			
			Double value = areaWAverages.get(s);
			if (value == null) {
				value = 0.0;
			}
			areaWAverages.put(s, value + (s.getValue(eCatchment) * eCatchment.getArea()) );			
		}
		totalArea += eCatchment.getArea();
	}
	
	private void updateStat(HashMap<ECatchment.ECatchmentStat, Double> stats, ECatchment.ECatchmentStat stat, Double value) {
		if (Double.isNaN(value)) return;
		if (!stats.containsKey(stat)) {
			stats.put(stat, value);
			return;
		}
		if (stat == ECatchmentStat.MIN_ELEVATION || stat == ECatchmentStat.MIN_SLOPE) {
			if (value < stats.get(stat)) {
				stats.put(stat, value);	
			}
			return;
		}
		if (stat == ECatchmentStat.MAX_ELEVATION || stat == ECatchmentStat.MAX_SLOPE || stat == ECatchmentStat.MAX_D2W_2D) {
			if (value > stats.get(stat)) {
				stats.put(stat, value);	
			}
			return;
		}
	}
	
}
