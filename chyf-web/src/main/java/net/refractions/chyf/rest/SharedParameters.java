package net.refractions.chyf.rest;

import net.refractions.chyf.ChyfDatastore;


public class SharedParameters {
	private String callback = "jsonp";
	private int srs = 4326;
	private Integer maxFeatures = null;
	private Double scale = null;
	private boolean removeHoles = false;

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

	public Double getScale() {
		return scale;
	}

	public void setScale(Double scale) {
		this.scale = scale;
	}

	public boolean getRemoveHoles() {
		return removeHoles;
	}

	public void setRemoveHoles(boolean removeHoles) {
		this.removeHoles = removeHoles;
	}

	public void resolveAndValidate() {
		if(maxFeatures == null || maxFeatures > ChyfDatastore.MAX_RESULTS) {
			maxFeatures = ChyfDatastore.MAX_RESULTS;
		}
	}
}