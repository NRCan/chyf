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

/**
 * Stores the results of the distance 2d processing
 * @author Emily
 *
 */
public class Distance2DResult {

	private HashMap<String, Statistics> results;
	
	public Distance2DResult() {
		results = new HashMap<>();
	}
	
	public void addResult(String fid, double mean, double max) {
		results.put(fid,  new Statistics(mean, max));
	}
	
	public Statistics getResult(String fid) {
		return results.get(fid);
	}
	
	public class Statistics{
		private double mean;
		private double max;
	
		public Statistics(double mean, double max) {
			this.max = max;
			this.mean = mean;
		}
		
		public double getMean() {
			return this.mean;
		}
		public double getMax() {
			return this.max;
		}
	}
	
	
}
