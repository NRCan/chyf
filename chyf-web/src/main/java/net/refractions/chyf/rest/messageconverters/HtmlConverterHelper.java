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
import java.io.Writer;

import org.apache.commons.text.StringEscapeUtils;
import org.locationtech.jts.geom.Geometry;

import net.refractions.chyf.hygraph.EFlowpath;

public class HtmlConverterHelper extends ConverterHelper {
	
	public HtmlConverterHelper(Writer out) {
		super(out);
	}
	
	private void startTable() throws IOException {
		out.write("<table style='border: 1px solid black'>");
	}

	private void endTable() throws IOException {
		out.write("</table>");
	}

	public void responseHeader(ApiResponse response) throws IOException {
		out.write(("<!DOCTYPE html><html><head><title>Response</title><meta charset=\"UTF-8\"></head><body>"));
	}

	public void responseFooter(ApiResponse response) throws IOException {
		out.write("</body></html>");
	}

	@Override
	protected void nullData() throws IOException {
		// TODO
	}

	@Override
	protected void field(String fieldName, boolean fieldValue) throws IOException {
		field(fieldName, Boolean.toString(fieldValue));
	}

	@Override
	protected void field(String fieldName, double fieldValue) throws IOException {
		field(fieldName, METRE_FORMAT.format(fieldValue));
	}

	@Override
	protected void field(String fieldName, Long fieldValue) throws IOException {
		if(fieldValue == null) {
			field(fieldName, "null");
		} else {
			field(fieldName, fieldValue.toString());
		}
	}

	@Override
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
		startTable();
	}
	
	@Override
	protected void objectFooter() throws IOException {
		endTable();	
	}
	
	@Override
	protected void listHeader() throws IOException {
	}
	
	@Override
	protected void listFooter() throws IOException {
	}
	
	@Override
	protected void nestedFieldHeader(String fieldName) throws IOException {
		out.write("<tr><td>" + fieldName + ":</td><td>");
	}
	
	@Override
	protected void nestedFieldFooter() throws IOException {
		out.write("</td></tr>");
	}

	@Override
	protected void featureCollectionHeader(ApiResponse responseMetadata) throws IOException {
		objectHeader();
		responseMetadata(responseMetadata);
		nestedFieldHeader("Features");
		listHeader();
	}

	@Override
	protected void featureCollectionFooter() throws IOException {
		listFooter();
		nestedFieldFooter();
		objectFooter();
	}

	@Override
	protected void featureHeader(Geometry g, Integer id, ApiResponse responseMetadata) throws IOException {
		objectHeader();
		responseMetadata(responseMetadata);
		if(id != null) {
			field("ID", id);
		}
	}

	@Override
	protected void featureFooter() throws IOException {
		objectFooter();
	}

}
