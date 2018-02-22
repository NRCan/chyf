package net.refractions.chyf.rest.messageconverters;

import java.io.IOException;
import java.io.Writer;

import com.google.gson.stream.JsonWriter;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class JsonConverterHelper extends ConverterHelper {

	private JsonWriter jw;

	public JsonConverterHelper(Writer out) {
		super(out);
		jw = new JsonWriter(out);
	}
	
	@Override
	protected void responseHeader(ApiResponse response) throws IOException {
	}
	
	@Override
	protected void responseFooter(ApiResponse response) throws IOException {
	}

	@Override
	protected void nullData() throws IOException {
		jw.nullValue();
	}

	@Override
	protected void field(String fieldName, boolean fieldValue) throws IOException {
		jw.name(fieldName).value(fieldValue);
	}

	@Override
	protected void field(String fieldName, Long fieldValue) throws IOException {
		jw.name(fieldName).value(fieldValue);
	}

	@Override
	protected void field(String fieldName, double fieldValue) throws IOException {
		jw.name(fieldName).jsonValue(METRE_FORMAT.format(fieldValue));
	}

	@Override
	protected void field(String fieldName, String fieldValue) throws IOException {
		jw.name(fieldName).value(fieldValue);
	}
	
	@Override
	protected void featureCollectionHeader(ApiResponse responseMetadata) throws IOException {
		jw.beginObject();
		jw.name("type").value("FeatureCollection");
		responseMetadata(responseMetadata);
		jw.name("features");
		jw.beginArray();
	}

	@Override
	protected void featureCollectionFooter() throws IOException {
		jw.endArray();
		jw.endObject();
	}

	@Override
	protected void featureHeader(Geometry g, Integer id, ApiResponse responseMetadata) throws IOException {
		jw.beginObject();
		jw.name("type").value("Feature");
		responseMetadata(responseMetadata);
		if(id != null) {
			jw.name("ID").value(id);
		}
		jw.name("geometry");
		geometry(g);
		jw.name("properties");
		jw.beginObject();
	}

	@Override
	protected void featureFooter() throws IOException {
		jw.endObject();
		jw.endObject();
	}

	@Override
	protected void objectHeader() throws IOException {
		jw.beginObject();
	}

	@Override
	protected void objectFooter() throws IOException {
		jw.endObject();
	}

	@Override
	protected void listHeader() throws IOException {
		jw.beginArray();
	}

	@Override
	protected void listFooter() throws IOException {
		jw.endArray();
	}

	@Override
	protected void nestedFieldHeader(String fieldName) throws IOException {
		jw.name(fieldName);
	}

	@Override
	protected void nestedFieldFooter() throws IOException {
	}

	private void geometry(Geometry g) throws IOException {
		jw.beginObject();
		jw.name("type").value(g.getGeometryType());
		jw.name("coordinates");
		switch(g.getGeometryType()) {
			case "Point":
				coordinate(((Point)g).getX(), ((Point)g).getY());
				break;
			case "LineString":
				coordinates(((LineString)g).getCoordinateSequence());
				break;
			case "Polygon":
				polygon(((Polygon)g)); 
				break;
			case "MultiPolygon":
				multiPolygon(((MultiPolygon)g)); 
				break;
			default: jw.value("Unknown geometry type");
		}
		jw.endObject();

	}

	private void multiPolygon(MultiPolygon mp) throws IOException {
		jw.beginArray();
		for(int i = 0; i < mp.getNumGeometries(); i++) {
			polygon((Polygon)mp.getGeometryN(i));
		}
		jw.endArray();
	}

	private void polygon(Polygon p) throws IOException {
		jw.beginArray();
		coordinates(p.getExteriorRing().getCoordinateSequence());
		for(int i = 0; i < p.getNumInteriorRing(); i++) {
			coordinates(p.getInteriorRingN(i).getCoordinateSequence());
		}
		jw.endArray();
	}

	private void coordinates(CoordinateSequence cs) throws IOException {
		jw.beginArray();
		for(int i = 0; i < cs.size(); i++) {
			coordinate(cs.getX(i), cs.getY(i));
		}
		jw.endArray();		
	}

	private void coordinate(double x, double y) throws IOException {
		jw.beginArray();
		jw.jsonValue(formatOrdinate(x));
		jw.jsonValue(formatOrdinate(y));
		jw.endArray();		
	}

}
