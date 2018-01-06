package net.refractions.chyf.rest;


public class SharedParameters {
	private String callback = "jsonp";
	private int srs = 4326;
	private Integer maxFeatures;

	public String getCallback() {
		return callback;
	}

	public void setCallback(String callback) {
		this.callback = callback;
	}

	public int getSrs() {
		return srs;
	}

	public void setOutputSRS(int srs) {
		this.srs = srs;
	}

	public Integer getMaxFeatures() {
		return maxFeatures;
	}

	public void setMaxFeatures(int maxFeatures) {
		// Clamp
		if(maxFeatures < 1) {
			maxFeatures = 1;
		}
		this.maxFeatures = maxFeatures;
	}	

}