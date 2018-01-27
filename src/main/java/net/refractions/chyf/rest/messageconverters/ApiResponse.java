package net.refractions.chyf.rest.messageconverters;

import net.refractions.chyf.rest.SharedParameters;


public class ApiResponse {
	
	private Object data;
	private int srs;
	private long executionTime;
	private String errorMsg;
	private String callback = "jsonp";
	private Double scale = null;

	public ApiResponse(Object data) {
		this.data = data;
	}
	
	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public int getSrs() {
		return srs;
	}

	public void setSrs(int srs) {
		this.srs = srs;
	}

	public void setParams(SharedParameters params) {
		callback = params.getCallback();
		setSrs(params.getSrs());
		setScale(params.getScale());
	}
	
	public boolean isError() {
		return errorMsg != null;
	}
	
	public String getErrorMsg() {
		return errorMsg;
	}
	
	public void setErrorMsg(String msg) {
		this.errorMsg = msg;
	}

	public String getCallback() {
		return callback;
	}

	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
	}

	public long getExecutionTime() {
		return executionTime;
	}

	public Double getScale() {
		return scale;
	}

	public void setScale(Double scale) {
		this.scale = scale;
	}

}
