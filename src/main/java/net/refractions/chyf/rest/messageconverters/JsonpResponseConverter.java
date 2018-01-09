/**
 * Copyright 2008-2015, Province of British Columbia
 * All rights reserved.
 */
package net.refractions.chyf.rest.messageconverters;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;

public class JsonpResponseConverter extends JsonResponseConverter {

	public JsonpResponseConverter() {
		super(new MediaType("application", "javascript",
				Charset.forName("UTF-8")));
	}

	@Override
	protected void writeInternal(ApiResponse response,
			HttpOutputMessage outputMessage) throws IOException,
			HttpMessageNotWritableException {
		Writer out = new OutputStreamWriter(outputMessage.getBody(), "UTF-8");
		JsonpConverterHelper helper = new JsonpConverterHelper(out);
		helper.convertResponse(response);
		out.flush();
	}

}
