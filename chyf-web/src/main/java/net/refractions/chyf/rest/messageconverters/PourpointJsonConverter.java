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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.hygraph.DrainageArea;
import net.refractions.chyf.hygraph.ECatchment;
import net.refractions.chyf.pourpoint.Pourpoint;
import net.refractions.chyf.pourpoint.PourpointEngine;
import net.refractions.chyf.pourpoint.PourpointOutput;
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
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.DISTANCE_PRIMARY)){
			writeRelationship(PourpointEngine.OutputType.DISTANCE_PRIMARY, result.getProjectedPourpointPrimaryDistanceMatrix());
		}
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.CATCHMENTS)){
			writeCatchments(response);
		}
		
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.CATCHMENT_CONTAINMENT)) {
			writeCatchmentContainment(response);
		}
		
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.SUBCATCHMENTS)){
			writePartitionedCatchments(response);
		}
		
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.SUBCATCHMENT_RELATIONSHIP)){
			String[] headers =new String[result.getPoints().size()];
			for (int i = 0; i < headers.length; i ++) {
				headers[i] = result.getPoints().get(i).getId();
			}
			writeRelationship(PourpointEngine.OutputType.SUBCATCHMENT_RELATIONSHIP, headers, result.getSubCatchmentRelationship());
		}
		
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.PARTITIONED_CATCHMENTS)){
			writeTraversalCompliantCatchments(response);
		}
		
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.PARTITIONED_CATCHMENT_RELATION)){
			List<DrainageArea> items = result.getPartitionedCatchments();
			String[] headers =new String[items.size()];
			for (int i = 0; i < headers.length; i ++) {
				headers[i] = items.get(i).getId();
			}
			writeRelationship(PourpointEngine.OutputType.PARTITIONED_CATCHMENT_RELATION, headers, result.getPartitionedCatchmentRelationship());
		}
		
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.INTERIOR_CATCHMENT)) {
			writeInteriorCatchment(response);
		}		
		
		if (result.getAvailableOutputs().contains(PourpointEngine.OutputType.PRT)) {
			writePrt();
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
			this.featureHeader(reproject(g.getGeometry(), response.getSrs()), counter, null);
			this.field("id", counter++);
			this.field("area", g.getArea() / 10_000);
			if (response.includeStats() && g.hasStats()) {
				for (ECatchment.ECatchmentStat s : ECatchment.ECatchmentStat.values()) {
					Double v = g.getStat(s);
					if (v != null) nfield(s.getFieldName().toLowerCase(), v);
				}
			}
			this.featureFooter();
		}
		this.featureCollectionFooter();
		
	}
	
	private void writeProjectedPoutpoint(ApiResponse response) throws IOException {
		this.featureCollectionHeader(response, PourpointEngine.OutputType.OUTPUT_PP);
		int counter = 1;
		for (Pourpoint p : result.getPoints()) {
			this.featureHeader(reproject(p.getProjectedPoint(), response.getSrs()), counter++, null);
			this.field("id", p.getId());
			this.field("ccode", p.getCcode());
			Point rawPoint =reproject(p.getRawPoint(), response.getSrs());
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
			this.featureHeader(reproject(g.getGeometry(), response.getSrs()), counter++, null);
			this.field("id", p.getId());
			this.field("area", g.getArea() / 10_000);
			
			if (response.includeStats() && g.hasStats()) {
				for (ECatchment.ECatchmentStat s : ECatchment.ECatchmentStat.values()) {
					Double v = g.getStat(s);
					if (v != null) nfield(s.getFieldName().toLowerCase(), v);
				}
			}
			
			this.featureFooter();
		}
		this.featureCollectionFooter();
		
	}
	
	private void writeTraversalCompliantCatchments(ApiResponse response) throws IOException {
		this.featureCollectionHeader(response, PourpointEngine.OutputType.PARTITIONED_CATCHMENTS);
		int counter = 1;
		Collection<DrainageArea> items =  result.getPartitionedCatchments();
		for (DrainageArea g : items) {
			this.featureHeader(reproject(g.getGeometry(), response.getSrs()), counter++, null);
			this.field("id", g.getId());
			this.field("area", g.getArea() / 10_000);
			if (response.includeStats() && g.hasStats()) {
				for (ECatchment.ECatchmentStat s : ECatchment.ECatchmentStat.values()) {
					Double v = g.getStat(s);
					if (v != null) nfield(s.getFieldName().toLowerCase(), v);
				}
			}
			this.featureFooter();
		}
		this.featureCollectionFooter();
	}
	
	
	private void writePartitionedCatchments(ApiResponse response) throws IOException {
		this.featureCollectionHeader(response, PourpointEngine.OutputType.SUBCATCHMENTS);
		int counter = 1;
		for (Pourpoint p : result.getPoints()) {
			DrainageArea g = result.getSubcatchment(p);
			this.featureHeader(reproject(g.getGeometry(), response.getSrs()), counter++, null);
			this.field("id", p.getId());
			this.field("area", g.getArea() / 10_000);
			if (response.includeStats() && g.hasStats()) {
				for (ECatchment.ECatchmentStat s : ECatchment.ECatchmentStat.values()) {
					Double v = g.getStat(s);
					if (v != null) nfield(s.getFieldName().toLowerCase(), v);
				}
			}
			this.featureFooter();
		}
		this.featureCollectionFooter();
		
	}
	
	private void writePrt() throws IOException{
		PourpointEngine.OutputType layer = PourpointEngine.OutputType.PRT;
		
		jw.beginObject();
		field("key", layer.key);
		field("name", layer.layername);
		field("tree", result.getPointRelationshipTree());
		jw.endObject();
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
	
	private <T extends Geometry> T reproject(T geom, int tosrs) {
		return (T)GeotoolsGeometryReprojector.reproject(geom, ChyfDatastore.BASE_CRS, GeotoolsGeometryReprojector.srsCodeToCRS(tosrs));
	}
}
