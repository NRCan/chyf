package net.refractions.chyf.rest.messageconverters;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import net.refractions.chyf.pourpoint.Pourpoint;
import net.refractions.chyf.pourpoint.PourpointEngine;
import net.refractions.chyf.pourpoint.PourpointOutput;
import net.refractions.chyf.pourpoint.UniqueSubCatchment;
import net.refractions.chyf.rest.GeotoolsGeometryReprojector;

public class PourpointJsonConverter extends JsonConverterHelper {

	protected Writer out;
	
	private PourpointOutput result;
	
	public PourpointJsonConverter(Writer out) {
		super(out);
	}
	
	@Override
	public void convertResponse(ApiResponse response) throws IOException {
		result = (PourpointOutput) response.getData();

		//		responseMetadata(responseMetadata);
		jw.beginArray();
		
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.PROJECTED)){
			writeProjectedPoutpoint(response);
		}
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.DISTANCE_MIN)){
			writeRelationship(PourpointEngine.OutputType.DISTANCE_MIN, result.getPourpointMinDistanceMatrix());
		}
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.DISTANCE_MAX)){
			writeRelationship(PourpointEngine.OutputType.DISTANCE_MAX, result.getPourpointMaxDistanceMatrix());
		}
		
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.PP_RELATIONSHIP)){
			String[] headers =new String[result.getPoints().size()];
			for (int i = 0; i < headers.length; i ++) {
				headers[i] = result.getPoints().get(i).getId();
			}
			writeRelationship(PourpointEngine.OutputType.PP_RELATIONSHIP, headers, result.getPourpointRelationship());
		}
		
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.UNIQUE_SUBCATCHMENTS_RELATION)){
			List<UniqueSubCatchment> items = result.getUniqueSubCatchments();
			String[] headers =new String[items.size()];
			for (int i = 0; i < headers.length; i ++) {
				headers[i] = items.get(i).getId();
			}
			writeRelationship(PourpointEngine.OutputType.UNIQUE_SUBCATCHMENTS_RELATION, headers, result.getPourpointCatchmentRelationship());
		}
		
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.CATCHMENTS)){
			writeCatchments(response);
		}
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.UNIQUE_CATCHMENTS)){
			writeUniqueCatchments(response);
		}
		
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.UNIQUE_SUBCATCHMENTS)){
			writeUniqueSubCatchments(response);
		}
		
		jw.endArray();
	}
	
	protected void featureCollectionHeader(ApiResponse responseMetadata,  PourpointEngine.OutputType layername) throws IOException {
		jw.beginObject();
		jw.name("type").value("FeatureCollection");
		jw.name("name").value(layername.key);

		jw.name("features");
		jw.beginArray();
	}
	
	private void writeProjectedPoutpoint(ApiResponse response) throws IOException {
		this.featureCollectionHeader(response, PourpointEngine.OutputType.PROJECTED);
		int counter = 1;
		for (Pourpoint p : result.getPoints()) {
			this.featureHeader(GeotoolsGeometryReprojector.reproject(p.getProjectedPoint(), response.getSrs()), counter++, null);
			this.field("id", p.getId());
			this.field("ccode", p.getCcode());
			Point rawPoint = GeotoolsGeometryReprojector.reproject(p.getPoint(), response.getSrs());
			jw.name("raw_x").value(rawPoint.getX());
			jw.name("raw_y").value(rawPoint.getY());
			this.featureFooter();
		}
		this.featureCollectionFooter();
		
	}
	
	private void writeCatchments(ApiResponse response) throws IOException {
		this.featureCollectionHeader(response, PourpointEngine.OutputType.CATCHMENTS);
		int counter = 1;
		for (Pourpoint p : result.getPoints()) {
			Geometry g = result.getCatchment(p);
			this.featureHeader(GeotoolsGeometryReprojector.reproject(g, response.getSrs()), counter++, null);
			this.field("id", p.getId());
			this.field("area", (double)g.getUserData());
			this.featureFooter();
		}
		this.featureCollectionFooter();
		
	}
	
	private void writeUniqueSubCatchments(ApiResponse response) throws IOException {
		this.featureCollectionHeader(response, PourpointEngine.OutputType.UNIQUE_SUBCATCHMENTS);
		int counter = 1;
		for (Pourpoint p : result.getPoints()) {
			Collection<UniqueSubCatchment> items =  result.getUniqueSubCatchments(p);
			for (UniqueSubCatchment i : items) {
				Geometry g = i.getGeometry();
				this.featureHeader(GeotoolsGeometryReprojector.reproject(g, response.getSrs()), counter++, null);
				this.field("id", i.getId());
				this.field("area", (double)g.getUserData());
				this.featureFooter();
			}
		}
		this.featureCollectionFooter();
		
	}
	
	
	private void writeUniqueCatchments(ApiResponse response) throws IOException {
		this.featureCollectionHeader(response, PourpointEngine.OutputType.UNIQUE_CATCHMENTS);
		int counter = 1;
		for (Pourpoint p : result.getPoints()) {
			Geometry g = result.getUniqueCatchment(p);
			this.featureHeader(GeotoolsGeometryReprojector.reproject(g, response.getSrs()), counter++, null);
			this.field("id", p.getId());
			this.field("area", (double)g.getUserData());
			this.featureFooter();
		}
		this.featureCollectionFooter();
		
	}
	
	private void writeRelationship(PourpointEngine.OutputType layer, Double[][] values) throws IOException{
		jw.beginObject();
		field("name", layer.key);
		
		jw.name("headers");
		this.listHeader();
		for (Pourpoint p : result.getPoints()) {
			jw.value(p.getId());
		}
		this.listFooter();
		
		jw.name("values");
		listHeader();
		for (int i = 0; i < values.length; i ++) {
			listHeader();
			for (int j = 0; j < values.length; j ++) {
				jw.value(values[i][j]);
			}
			listFooter();
		}
		
		listFooter();
		jw.endObject();
	}
	
	private void writeRelationship(PourpointEngine.OutputType layer, String[] headers, Integer[][] values) throws IOException{
		jw.beginObject();
		field("name", layer.key);
		
		jw.name("headers");
		this.listHeader();
		for (String h : headers) {
			jw.value(h);
		}
		this.listFooter();
		
		jw.name("values");
		listHeader();
		for (int i = 0; i < values.length; i ++) {
			listHeader();
			for (int j = 0; j < values.length; j ++) {
				jw.value(values[i][j]);
			}
			listFooter();
		}
		
		listFooter();
		jw.endObject();
	}
	
}
