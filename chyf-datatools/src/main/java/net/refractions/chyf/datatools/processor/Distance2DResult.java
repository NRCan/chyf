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
