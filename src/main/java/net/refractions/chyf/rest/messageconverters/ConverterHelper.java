package net.refractions.chyf.rest.messageconverters;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;

import net.refractions.chyf.hydrograph.EFlowpath;

public abstract class ConverterHelper {
	static final DecimalFormat DEGREE_FORMAT = new DecimalFormat("###.#####");
	static final DecimalFormat METRE_FORMAT = new DecimalFormat("###.##");
	
	protected Writer out;
	
	public ConverterHelper(Writer out) {
		this.out = out;
	}

	protected abstract void writeHeader(ApiResponse response) throws IOException;
	protected abstract void writeFooter(ApiResponse response) throws IOException;
	
	protected abstract void writeField(String fieldName, int fieldValue) throws IOException;
	protected abstract void writeField(String fieldName, String fieldValue) throws IOException;

	protected void writeFields(EFlowpath eFlowpath) throws IOException {
		writeField("ID", eFlowpath.getId());
	}
	
	public void convertResponse(ApiResponse response) 
			throws IOException {
		writeHeader(response);
		Object data = response.getData();
		if(data instanceof EFlowpath) {
			writeFields((EFlowpath)data);
		}
		writeFooter(response);
	}
	
	protected static String formatOrdinate(double ord) {
		if(ord <= 180 && ord >= -180) {
			return DEGREE_FORMAT.format(ord);
		}
		return METRE_FORMAT.format(ord);
	}

}
