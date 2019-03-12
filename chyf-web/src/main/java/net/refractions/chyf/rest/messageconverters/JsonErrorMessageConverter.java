/*
 * Copyright 2019 Government of Canada
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package net.refractions.chyf.rest.messageconverters;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.google.gson.stream.JsonWriter;

import net.refractions.chyf.rest.exceptions.ErrorMessage;

/**
 * Supports more than just JSON output types, this is the default exception format.
 * 
 * @author chodgson
 * 
 */
public class JsonErrorMessageConverter extends AbstractHttpMessageConverter<ErrorMessage> {
	
	public JsonErrorMessageConverter() {
		super(MediaType.APPLICATION_XHTML_XML,
				MediaType.TEXT_HTML,
				MediaType.APPLICATION_JSON,
				new org.springframework.http.MediaType("application", "javascript",
						Charset.forName("UTF-8")));
	}
	
	@Override
	protected boolean supports(Class<?> clazz) {
		return ErrorMessage.class.isAssignableFrom(clazz);
	}
	
	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		return false;
	}
	
	@Override
	protected ErrorMessage readInternal(Class<? extends ErrorMessage> clazz,
			HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
		return null;
	}
	
	@Override
	protected void writeInternal(ErrorMessage message, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		outputMessage.getHeaders().setContentType(MediaType.TEXT_HTML);
		Writer out = new OutputStreamWriter(outputMessage.getBody(), "UTF-8");
		
		try(JsonWriter jw = new JsonWriter(out)){
			jw.beginObject();
			jw.name("error");
			jw.value(message.getMessage());
			jw.endObject();
			out.flush();
		}		
		
	}
	
}
