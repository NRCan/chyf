package net.refractions.chyf.rest.messageconverters;

import java.io.IOException;
import java.io.Writer;

import com.google.gson.stream.JsonWriter;

public class JsonConverterHelper extends ConverterHelper {

	private JsonWriter jw;

	public JsonConverterHelper(Writer out) {
		super(out);
		jw = new JsonWriter(out);
	}
	
	protected void writeHeader(ApiResponse response) throws IOException {
		jw.beginObject();
	}
	
	protected void writeFooter(ApiResponse response) throws IOException {
		jw.endObject();
	}
	
	protected void writeField(String fieldName, int fieldValue) throws IOException {
		jw.name(fieldName).value(fieldValue);
	}

	protected void writeField(String fieldName, String fieldValue) throws IOException {
		jw.name(fieldName).value(fieldValue);
	}

}
