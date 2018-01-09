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
	
	@Override
	protected void writeResponseHeader(ApiResponse response) throws IOException {
		jw.beginObject();
	}
	
	@Override
	protected void writeResponseFooter(ApiResponse response) throws IOException {
		jw.endObject();
	}
	
	@Override
	protected void writeField(String fieldName, int fieldValue) throws IOException {
		jw.name(fieldName).value(fieldValue);
	}

	@Override
	protected void writeField(String fieldName, String fieldValue) throws IOException {
		jw.name(fieldName).value(fieldValue);
	}

	@Override
	protected void writeObjectHeader() throws IOException {
		jw.beginObject();
	}

	@Override
	protected void writeObjectFooter() throws IOException {
		jw.endObject();
	}

	@Override
	protected void writeListHeader() throws IOException {
		jw.beginArray();
	}

	@Override
	protected void writeListFooter() throws IOException {
		jw.endArray();
	}

	@Override
	protected void writeNestedFieldHeader(String fieldName) throws IOException {
		jw.name(fieldName);
	}

	@Override
	protected void writeNestedFieldFooter() throws IOException {
	}

}
