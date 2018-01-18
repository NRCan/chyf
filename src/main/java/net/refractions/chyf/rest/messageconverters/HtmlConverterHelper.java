package net.refractions.chyf.rest.messageconverters;

import java.io.IOException;
import java.io.Writer;

import net.refractions.chyf.hygraph.EFlowpath;

import org.apache.commons.lang3.StringEscapeUtils;

import com.vividsolutions.jts.geom.Geometry;

public class HtmlConverterHelper extends ConverterHelper {
	
	public HtmlConverterHelper(Writer out) {
		super(out);
	}
	
	public void responseHeader(ApiResponse response) throws IOException {
		out.write(("<!DOCTYPE html><html><head><title>Router Response</title></head><body><table>"));
	}

	public void responseFooter(ApiResponse response) throws IOException {
		out.write("</table></body></html>");
	}

	@Override
	protected void nullData() throws IOException {
		// TODO
	}

	@Override
	protected void field(String fieldName, double fieldValue) throws IOException {
		field(fieldName, METRE_FORMAT.format(fieldValue));
	}

	protected void field(String fieldName, long fieldValue) throws IOException {
		field(fieldName, Long.toString(fieldValue));
	}

	protected void field(String fieldName, String fieldValue) throws IOException {
		out.write("<tr><td>" + fieldName + ":</td>");		
		out.write("<td>" + escape(fieldValue) + "</td></tr>\n");
	}

	protected void writeFields(EFlowpath eFlowpath) throws IOException {
		field("ID", eFlowpath.getId());
	}
	
	/**
	 * Escapes a single value
	 * 
	 * @param field the value to escape
	 */
	static String escape(Object field)
	{
		if(field == null) {
			return "";
		}
		return StringEscapeUtils.escapeXml10(field.toString());
	}

	@Override
	protected void objectHeader() throws IOException {
		out.write("<tr><td><table>");
	}
	
	@Override
	protected void objectFooter() throws IOException {
		out.write("</table></td></tr>");	
	}
	
	@Override
	protected void listHeader() throws IOException {
		out.write("<table>");
	}
	
	@Override
	protected void listFooter() throws IOException {
		out.write("</table>");
	}
	
	@Override
	protected void nestedFieldHeader(String fieldName) throws IOException {
		out.write("<tr><td>" + fieldName + "</td><td>");
	}
	
	@Override
	protected void nestedFieldFooter() throws IOException {
		out.write("</td></tr>");
	}

	@Override
	protected void featureCollectionHeader() throws IOException {
		listHeader();
	}

	@Override
	protected void featureCollectionFooter() throws IOException {
		listFooter();
	}

	@Override
	protected void featureHeader(Geometry g, Integer id) throws IOException {
		objectHeader();
		if(id != null) {
			field("ID", id);
		}
	}

	@Override
	protected void featureFooter() throws IOException {
		objectFooter();
	}

}
