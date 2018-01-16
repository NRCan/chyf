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

	protected void field(String fieldName, int fieldValue) throws IOException {
		out.write("<tr><td>" + fieldName + ":</td>");		
		out.write("<td>" + fieldValue + "</td></tr>\n");
	}

	protected void field(String fieldName, String fieldValue) throws IOException {
		out.write("<tr><td>" + fieldName + ":</td>");		
		out.write("<td>" + escape(fieldValue) + "</td></tr>\n");
	}

	protected void writeFields(EFlowpath eFlowpath) throws IOException {
		field("ID", eFlowpath.getId());
	}
	
//		writeField("routeDescription", response.getRouteDescription());
//		writeField("searchTimestamp", response.getTimeStamp());
//		writeField("executionTime", response.getExecutionTime());
//		if(response instanceof RouterOptimizedResponse) {
//			writeField("routingExecutionTime", ((RouterOptimizedResponse)response).getRoutingExecutionTime());
//			writeField("optimizationExecutionTime", ((RouterOptimizedResponse)response).getOptimizationExecutionTime());
//		}
//		writeField("version", RouterConfig.VERSION);
//		writeField("disclaimer", config.getDisclaimer());
//		writeField("privacyStatement", config.getPrivacyStatement());
//		writeField("copyrightNotice", config.getCopyrightNotice());
//		writeField("copyrightLicense", config.getCopyrightLicense());
//		writeField("srsCode", response.getSrsCode());
//		writeField("criteria", response.getCriteria().toString());		
//		writeField("distanceUnit", response.getDistanceUnit().abbr());
//	}
//	
//	protected void writeFields(RouterDistanceResponse response) throws IOException {
//		writeFields((ApiResponse)response);
//		writeField("points", buildPointListTable(response.getPoints()), false);
//		writeField("routeFound", response.isRouteFound());
//		writeField("distance", response.getDistanceStr());
//		writeField("time", response.getTime());
//		writeField("timeText", TimeHelper.formatTime(response.getTime()));
//	}
//
//	protected void writeFields(RouterDistanceBetweenPairsResponse response) throws IOException {
//		writeFields((ApiResponse)response);
//		writeField("fromPoints", buildPointListTable(response.getFromPoints()), false);
//		writeField("toPoints", buildPointListTable(response.getToPoints()), false);
//		writeFieldName("pairs", "pairs");
//		writeFieldValueHeader();
//		boolean skipFails = response.getMaxPairs() < Integer.MAX_VALUE;
//		int curResult = 0;
//		for(int from = 0; from < response.getFromPoints().size(); from++) {
//			for( int to = 0; to < response.getToPoints().size(); to++) {
//				// if we are skipping fails, only output non-fails
//				if(!skipFails || response.getError(curResult) == null) {
//					out.write("<table>");
//					writeField("fromElement", from);
//					writeField("toElement", to);
//					writeField("distance", response.getDistanceStr(curResult));
//					writeField("time", response.getTime(curResult));
//					writeField("timeText", TimeHelper.formatTime(response.getTime(curResult)));
//					String message = response.getError(curResult);
//					if(message != null) {
//						writeField("message", message);
//					}
//					out.write("</table>");
//				}
//				curResult++;
//			}
//		}
//		writeFieldValueFooter();
//	}
//
//	protected void writeFields(RouterRouteResponse response) throws IOException {
//		writeFields((RouterDistanceResponse)response);
//		
//		CoordinateSequence coords = response.getPath().getCoordinateSequence();
//		StringBuilder routeStr = new StringBuilder("<table>");
//		for(int i = 0; i < coords.size(); i++) {
//			routeStr.append("<tr><td>" + formatOrdinate(coords.getX(i)) + "</td><td>" + formatOrdinate(coords.getY(i)) + "</td></tr>");
//		}
//		routeStr.append("</table>");
//		writeField("route", routeStr, false);		
//	}
//	
//	protected void writeFields(RouterDirectionsResponse response) throws IOException {
//		writeFields((RouterRouteResponse)response);
//		StringBuilder dirStr = new StringBuilder("<table>");
//		for(String direction : response.getDirections()) {
//			dirStr.append("<tr><td>" + escape(direction) + "</td></tr>");
//		}
//		dirStr.append("</table>");
//		writeField("directions", dirStr, false);		
//
//	}
//
//	static String buildPointListTable(List<Point> points) {
//		StringBuilder pointStr = new StringBuilder("<table>");
//		for(Point p : points) {
//			pointStr.append("<tr><td>" + formatOrdinate(p.getX()) + "</td><td>" + formatOrdinate(p.getY()) + "</td></tr>");
//		}
//		pointStr.append("</table>");
//		return pointStr.toString();
//	}
	
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
