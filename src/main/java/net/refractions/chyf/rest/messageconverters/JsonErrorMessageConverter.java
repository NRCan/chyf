package net.refractions.chyf.rest.messageconverters;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import net.refractions.chyf.rest.exceptions.ErrorMessage;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

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
		out.write("<!DOCTYPE html>\r\n<html>\r\n<head></head>\r\n<body>\r\n");
		
		out.write(message.getMessage());
		out.write("</body>\r\n</html>");
		out.flush();
	}
	
}
