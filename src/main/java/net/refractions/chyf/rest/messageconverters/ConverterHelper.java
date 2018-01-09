package net.refractions.chyf.rest.messageconverters;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.List;

import net.refractions.chyf.hydrograph.EFlowpath;

public abstract class ConverterHelper {
	static final DecimalFormat DEGREE_FORMAT = new DecimalFormat("###.#####");
	static final DecimalFormat METRE_FORMAT = new DecimalFormat("###.##");
	
	protected Writer out;
	
	public ConverterHelper(Writer out) {
		this.out = out;
	}

	protected abstract void writeResponseHeader(ApiResponse response) throws IOException;
	protected abstract void writeResponseFooter(ApiResponse response) throws IOException;
	protected abstract void writeObjectHeader() throws IOException;
	protected abstract void writeObjectFooter() throws IOException;
	protected abstract void writeListHeader() throws IOException;
	protected abstract void writeListFooter() throws IOException;
	protected abstract void writeNestedFieldHeader(String fieldName) throws IOException;
	protected abstract void writeNestedFieldFooter() throws IOException;
	
	protected abstract void writeField(String fieldName, int fieldValue) throws IOException;
	protected abstract void writeField(String fieldName, String fieldValue) throws IOException;

	protected void writeFields(EFlowpath eFlowpath) throws IOException {
		writeObjectHeader();
		writeField("ID", eFlowpath.getId());
		writeObjectFooter();
	}
	
	public void convertResponse(ApiResponse response) 
			throws IOException {
		writeResponseHeader(response);
		Object data = response.getData();
		if(data instanceof List<?>) {
			writeNestedFieldHeader("data");
			writeListHeader();
			for(Object o : ((List<?>)data)) {
				writeSingleDataObject(o);
			}
			writeListFooter();
			writeNestedFieldFooter();
		} else {
			writeNestedFieldHeader("data");
			writeSingleDataObject(data);
			writeNestedFieldFooter();
		}
		writeResponseFooter(response);
	}
	
	private void writeSingleDataObject(Object data) throws IOException {
		if(data instanceof EFlowpath) {
			writeFields((EFlowpath)data);
		}		
	}
	
	protected static String formatOrdinate(double ord) {
		if(ord <= 180 && ord >= -180) {
			return DEGREE_FORMAT.format(ord);
		}
		return METRE_FORMAT.format(ord);
	}

}
