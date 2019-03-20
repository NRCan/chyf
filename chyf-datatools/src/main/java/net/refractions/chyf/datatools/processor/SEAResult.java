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
package net.refractions.chyf.datatools.processor;

import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Stores the results of the slope/aspect/elevation computing.  Results
 * are stored per feature id.
 * 
 * @author Emily
 *
 */
public class SEAResult {

	private HashMap<String, Statistics> results;
	
	public SEAResult() {
		results = new HashMap<>();
	}
	
	public HashMap<String, Statistics> getStats(){
		return this.results;
	}
	
	public void addElevationValue(String fid, double elevation) {
		Statistics stats = this.results.get(fid);
		if (stats == null) {
			stats = new Statistics(elevation);
			this.results.put(fid, stats);
		}else {
			stats.addElevation(elevation);
		}
	}
	
	public void addSlopeAspectElevationValue(String fid, double slope, double aspect, double elevation) {
		Statistics stats = this.results.get(fid);
		if (stats == null) {
			stats = new Statistics(elevation, slope, aspect);
			this.results.put(fid, stats);
		}else {
			stats.addElevation(elevation);
			stats.addSlope(slope, aspect);
		}
	}
	
	public void merge(SEAResult other) {
		for (Entry<String,Statistics> e : other.results.entrySet()) {
			Statistics s = this.results.get(e.getKey());
			if (s == null) {
				this.results.put(e.getKey(), e.getValue());
			}else {
				s.merge(e.getValue());
			}
		}
	}
	
	
	public class Statistics{
		private double slopeSum;
		private int numItem;
		
		private double elvSum;
		private int numItemElv;
		
		private double maxElevation;
		private double minElevation;
		
		private double maxSlope;
		private double minSlope;
		
		//north, south, east, west, flat (slope < 5)
		private int[] aspectClassCnt = {0,0,0,0,0};

		Statistics(double elevation, double slope, double aspect){
			this.maxElevation = elevation;
			this.minElevation = elevation;
			this.maxSlope = slope;
			this.minSlope = slope;
			this.numItem = 1;
			this.numItemElv = 1;
			this.slopeSum = slope;
			this.elvSum = elevation;
			addAspect(slope, aspect);
		}
		
		Statistics(double elevation){
			this.maxElevation = elevation;
			this.minElevation = elevation;
			this.maxSlope = Double.MIN_VALUE;
			this.minSlope = Double.MAX_VALUE;
			
			this.numItem = 0;
			this.slopeSum = 0;
			
			this.numItemElv = 1;
			this.elvSum = elevation;
		}
		
		void merge(Statistics other){
			this.slopeSum += other.slopeSum;
			this.numItem += other.numItem;
			
			this.elvSum += other.elvSum;
			this.numItemElv += other.numItemElv;
			
			if (other.maxElevation > this.maxElevation) this.maxElevation = other.maxElevation;
			if (other.minElevation < this.minElevation) this.minElevation = other.minElevation;
			
			if (other.maxSlope > this.maxSlope) this.maxSlope = other.maxSlope;
			if (other.minSlope < this.minSlope) this.minSlope = other.minSlope;
			
			for (int i = 0; i < this.aspectClassCnt.length; i ++) {
				this.aspectClassCnt[i] = this.aspectClassCnt[i] + other.aspectClassCnt[i];
			}
		}
		
		
		void addElevation(double elevation) {
			if (elevation > this.maxElevation) this.maxElevation = elevation;
			if (elevation < this.minElevation) this.minElevation = elevation;
			this.numItemElv++;
			this.elvSum += elevation;
		}
		
		private void addAspect(double slope, double aspect) {
			if (aspect < 0 || aspect > 360) throw new IllegalStateException("invalid aspect value");
			int index = -1;
			if (slope < 3) {
				//flat
				index = 4;
			}else if (aspect >= 315 || aspect < 45) {
				//north
				index = 0;
			}else if (aspect >= 135 && aspect < 225) {
				//south
				index = 1;
			}else if (aspect >= 45 && aspect < 135) {
				//east
				index = 2;
			}else if (aspect >= 225 && aspect < 315) {
				//west
				index = 3;
			}
			aspectClassCnt[index] = aspectClassCnt[index] + 1;
		}
		void addSlope(double slope, double aspect) {
			if (slope > this.maxSlope) this.maxSlope = slope;
			if (slope < this.minSlope) this.minSlope = slope;
			this.numItem++;
			this.slopeSum += slope;
			addAspect(slope, aspect);
		}
		
		public double getAverageSlope() {
			if (numItem == 0) return Double.NaN;
			return slopeSum / numItem;
		}
		
		public double getAverageElevation() {
			if (numItemElv == 0) return Double.NaN;
			return elvSum / numItemElv;
		}
		
		public Double getMaxElevation() {
			return this.maxElevation;
		}
		public double getMinElevation() {
			return this.minElevation;
		}
		public double getMaxSlope() {
			return this.maxSlope;
		}
		public double getMinSlope() {
			return this.minSlope;
		}
		
		public double getNorthPercent() {
			return getPercent(0);
		}
		public double getSouthPercent() {
			return getPercent(1);
		}
		public double getEastPercent() {
			return getPercent(2);
		}
		public double getWestPercent() {
			return getPercent(3);
		}
		public double getFlatPercent() {
			return getPercent(4);
		}
		private double getPercent(int index) {
			return aspectClassCnt[index] / (double)(aspectClassCnt[0] + aspectClassCnt[1] + aspectClassCnt[2] + aspectClassCnt[3] + aspectClassCnt[4] );
		}
		
		public SEAResult.Statistics clone(){
			SEAResult.Statistics clone = new SEAResult.Statistics(-1);
			clone.slopeSum = this.slopeSum;
			clone.numItem = this.numItem;
			clone.elvSum = this.elvSum;
			clone.numItemElv = this.numItemElv;
			clone.maxElevation = this.maxElevation;
			clone.minElevation = this.minElevation;
			clone.maxSlope = this.maxSlope;
			clone.minSlope = this.minSlope;
			
			clone.aspectClassCnt = new int[this.aspectClassCnt.length];
			for (int i = 0; i < clone.aspectClassCnt.length; i ++) clone.aspectClassCnt[i] = this.aspectClassCnt[i];
			return clone;
		}
		
	}
}
