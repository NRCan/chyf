package net.refractions.chyf.rest.messageconverters;

import java.io.IOException;
import java.io.Writer;

public class JsonpConverterHelper extends JsonConverterHelper {
	
	public JsonpConverterHelper(Writer out) {
		super(out);
	}

	protected void writeHeader(ApiResponse response) throws IOException {
		out.write(response.getCallback() + "({");
	}
	
	protected void writeFooter(ApiResponse response) throws IOException {
		out.write("});");
	}

}
