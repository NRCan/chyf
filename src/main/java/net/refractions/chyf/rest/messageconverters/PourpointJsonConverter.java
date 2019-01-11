package net.refractions.chyf.rest.messageconverters;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.vividsolutions.jts.geom.Point;

import net.refractions.chyf.hygraph.DrainageArea;
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

		jw.beginArray();
		
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.OUTPUT_PP)){
			writeProjectedPoutpoint(response);
		}
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.DISTANCE_MIN)){
			writeRelationship(PourpointEngine.OutputType.DISTANCE_MIN, result.getProjectedPourpointMinDistanceMatrix());
		}
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.DISTANCE_MAX)){
			writeRelationship(PourpointEngine.OutputType.DISTANCE_MAX, result.getProjectedPourpointMaxDistanceMatrix());
		}
		
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.CATCHMENTS)){
			writeCatchments(response);
		}
		
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.CATCHMENT_CONTAINMENT)) {
			writeCatchmentContainment(response);
		}
		
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.NONOVERLAPPING_CATCHMENTS)){
			writeNonOverlappingCatchments(response);
		}
		
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.NONOVERLAPPINGCATCHMENT_RELATIONSHIP)){
			String[] headers =new String[result.getPoints().size()];
			for (int i = 0; i < headers.length; i ++) {
				headers[i] = result.getPoints().get(i).getId();
			}
			writeRelationship(PourpointEngine.OutputType.NONOVERLAPPINGCATCHMENT_RELATIONSHIP, headers, result.getNonOverlappingCatchmentRelationship());
		}
		
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.TRAVERSAL_COMPLIANT_CATCHMENTS)){
			writeTraversalCompliantCatchments(response);
		}
		
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.TRAVERSAL_COMPLIANT_CATCHMENT_RELATION)){
			List<UniqueSubCatchment> items = result.getTraversalCompliantCatchments();
			String[] headers =new String[items.size()];
			for (int i = 0; i < headers.length; i ++) {
				headers[i] = items.get(i).getId();
			}
			writeRelationship(PourpointEngine.OutputType.TRAVERSAL_COMPLIANT_CATCHMENT_RELATION, headers, result.getTraversalCompliantCatchmentRelationship());
		}
		
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.INTERIOR_CATCHMENT)) {
			writeInteriorCatchment(response);
		}		
		jw.endArray();
		jw.flush();
	}
	
	protected void featureCollectionHeader(ApiResponse responseMetadata,  PourpointEngine.OutputType layername) throws IOException {
		jw.beginObject();
		jw.name("type").value("FeatureCollection");
		jw.name("key").value(layername.key);
		jw.name("name").value(layername.layername);
		jw.name("features");
		jw.beginArray();
	}
	
	private void writeInteriorCatchment(ApiResponse response) throws IOException {
		this.featureCollectionHeader(response, PourpointEngine.OutputType.INTERIOR_CATCHMENT);
		int counter = 1;
		for (DrainageArea g : result.getInteriorCatchments()) {
			this.featureHeader(GeotoolsGeometryReprojector.reproject(g.getGeometry(), response.getSrs()), counter, null);
			this.field("id", counter++);
			this.field("area", g.getArea() / 10_000);
			this.featureFooter();
		}
		this.featureCollectionFooter();
		
	}
	
	private void writeProjectedPoutpoint(ApiResponse response) throws IOException {
		this.featureCollectionHeader(response, PourpointEngine.OutputType.OUTPUT_PP);
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
	
	private void writeCatchmentContainment(ApiResponse response) throws IOException {
		List<Pourpoint> pnts = new ArrayList<>(result.getPoints());
		pnts.sort((a,b)->a.getId().compareTo(b.getId()));
		String[] headers = new String[pnts.size()];
		for (int i = 0; i < pnts.size(); i ++) {
			headers[i] = pnts.get(i).getId();
		}
		writeRelationship(PourpointEngine.OutputType.CATCHMENT_CONTAINMENT, headers, result.getCatchmentContainment());
	}
	
	private void writeCatchments(ApiResponse response) throws IOException {
		this.featureCollectionHeader(response, PourpointEngine.OutputType.CATCHMENTS);
		int counter = 1;
		for (Pourpoint p : result.getPoints()) {
			DrainageArea g = result.getCatchment(p);
			this.featureHeader(GeotoolsGeometryReprojector.reproject(g.getGeometry(), response.getSrs()), counter++, null);
			this.field("id", p.getId());
			this.field("area", g.getArea() / 10_000);
			this.featureFooter();
		}
		this.featureCollectionFooter();
		
	}
	
	private void writeTraversalCompliantCatchments(ApiResponse response) throws IOException {
		this.featureCollectionHeader(response, PourpointEngine.OutputType.TRAVERSAL_COMPLIANT_CATCHMENTS);
		int counter = 1;
		for (Pourpoint p : result.getPoints()) {
			Collection<UniqueSubCatchment> items =  result.getTraversalCompliantCatchments(p);
			for (UniqueSubCatchment i : items) {
				DrainageArea g = i.getDrainageArea();
				this.featureHeader(GeotoolsGeometryReprojector.reproject(g.getGeometry(), response.getSrs()), counter++, null);
				this.field("id", i.getId());
				this.field("area", g.getArea() / 10_000);
				this.featureFooter();
			}
		}
		this.featureCollectionFooter();
		
	}
	
	
	private void writeNonOverlappingCatchments(ApiResponse response) throws IOException {
		this.featureCollectionHeader(response, PourpointEngine.OutputType.NONOVERLAPPING_CATCHMENTS);
		int counter = 1;
		for (Pourpoint p : result.getPoints()) {
			DrainageArea g = result.getNonOverlappingCatchments(p);
			this.featureHeader(GeotoolsGeometryReprojector.reproject(g.getGeometry(), response.getSrs()), counter++, null);
			this.field("id", p.getId());
			this.field("area", g.getArea() / 10_000);
			this.featureFooter();
		}
		this.featureCollectionFooter();
		
	}
	
	private void writeRelationship(PourpointEngine.OutputType layer, Double[][] values) throws IOException{
		jw.beginObject();
		field("key", layer.key);
		field("name", layer.layername);
		
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
		field("key", layer.key);
		field("name", layer.layername);
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
