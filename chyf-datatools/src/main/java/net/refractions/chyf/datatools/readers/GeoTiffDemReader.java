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
package net.refractions.chyf.datatools.readers;

import java.io.Closeable;
import java.nio.file.Path;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class GeoTiffDemReader implements Closeable {

	private GridCoverage2D coverage;
	private GeneralEnvelope bounds;
	private double xCellSize;
	private double yCellSize;
	
	private int xcells;
	private int ycells;
	
	private CoordinateReferenceSystem crs;
	
	public GeoTiffDemReader(Path file) throws Exception{

		AbstractGridFormat format = GridFormatFinder.findFormat( file.toFile() );
		    
		GridCoverage2DReader reader = format.getReader( file.toFile() );
		
		this.xcells = reader.getImageLayout().getWidth(null);
		this.ycells = reader.getImageLayout().getHeight(null);
		
		this.bounds = reader.getOriginalEnvelope();
		
		this.xCellSize = (bounds.getUpperCorner().getOrdinate(0) - bounds.getLowerCorner().getOrdinate(0)) / (double)xcells;
		this.yCellSize = (bounds.getUpperCorner().getOrdinate(1) - bounds.getLowerCorner().getOrdinate(1)) / (double)ycells;
		
		String[] names = reader.getGridCoverageNames();
		this.coverage = reader.read(names[0], new GeneralParameterValue[] {});
		
		this.crs = reader.getCoordinateReferenceSystem();
		
	}
	
	public GeneralEnvelope getFileBounds(){
		return this.bounds;
	}
	
	public double[] getNoData() {
		return this.coverage.getSampleDimension(0).getNoDataValues();
	}
	
	public CoordinateReferenceSystem getCrs() {
		return this.crs;
	}
	
	public double getXCellSize() {
		return this.xCellSize;
	}
	public double getYCellSize() {
		return this.yCellSize;
	}
	public GridCoverage2D getData(ReferencedEnvelope cropBounds) {
		
		CoverageProcessor processor = new CoverageProcessor();
		
		final ParameterValueGroup param = processor.getOperation("CoverageCrop").getParameters();
		
		double x1 = cropBounds.getLowerCorner().getOrdinate(0);
		double y1 = cropBounds.getLowerCorner().getOrdinate(1);
		
		int cellxmin = (int)Math.floor((cropBounds.getMinX() - x1) / xCellSize);
		int cellymin = (int)Math.floor((cropBounds.getMinY() - y1) / yCellSize);
		
		int cellxmax = (int)Math.ceil((cropBounds.getMaxX() - x1) / xCellSize);
		int cellymax = (int)Math.ceil((cropBounds.getMaxY() - y1) / yCellSize);
		
		GeneralEnvelope env = new GeneralEnvelope(new double[] {cellxmin * xCellSize + x1, cellymin * yCellSize + y1}, new double[] {cellxmax * xCellSize + x1, cellymax * yCellSize + y1});
		
		//truncate requested envelope to coverage bounds
		double xc = env.getMinimum(0);
		if (xc < this.bounds.getMinimum(0)) {
			xc = this.bounds.getMinimum(0);
		}
		
		double yc = env.getMinimum(1);
		if (yc < this.bounds.getMinimum(1)) {
			yc = this.bounds.getMinimum(1);
		}
		
		double xd = env.getMaximum(0);
		if (xd > this.bounds.getMaximum(0)) {
			xd = this.bounds.getMaximum(0);
		}
		double yd = env.getMaximum(1);
		if (yd > this.bounds.getMaximum(1)) {
			yd = this.bounds.getMaximum(1);
		}
		
		GeneralEnvelope env2 = new GeneralEnvelope(new double[] {xc,yc}, new double[] {xd,yd});
		
		final GeneralEnvelope crop = new GeneralEnvelope( env2 );
		param.parameter("Source").setValue(  coverage );
		param.parameter("Envelope").setValue( crop );

		GridCoverage2D cropped = (GridCoverage2D) processor.doOperation(param);
		return cropped;
	}
	
	@Override
	public void close() {
		this.coverage.dispose(true);
	}	
}
