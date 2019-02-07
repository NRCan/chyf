package net.refractions.chyf.rest.messageconverters;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

public class HtmlResponseConverter extends AbstractHttpMessageConverter<ApiResponse> {

	public HtmlResponseConverter() {
		super(MediaType.APPLICATION_XHTML_XML, MediaType.TEXT_HTML);
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		return ApiResponse.class.isAssignableFrom(clazz);
	}

	@Override
	protected ApiResponse readInternal(
			Class<? extends ApiResponse> clazz,
			HttpInputMessage inputMessage) throws IOException,
			HttpMessageNotReadableException {
		return null;
	}

	@Override
	protected void writeInternal(ApiResponse response,
			HttpOutputMessage outputMessage) throws IOException,
			HttpMessageNotWritableException {
		Writer out = new OutputStreamWriter(outputMessage.getBody(), "UTF-8");
		HtmlConverterHelper helper = new HtmlConverterHelper(out);
		helper.convertResponse(response);
		out.flush();
	}

}
