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

import java.io.IOException;

import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.geometry.jts.ReferencedEnvelope;


/**
 * CHyF data source reader that provides general functions for reading input data
 * from geotools feature readers.
 * 
 * @author Emily
 *
 */
public interface ChyfDataSource extends AutoCloseable {

	/**
	 * 
	 * @param bounds if null then entire dataset should be returned
	 * @return
	 * @throws IOException
	 */
	SimpleFeatureReader getECatchments(ReferencedEnvelope bounds) throws IOException;
	
	/**
	 * 
	 * @param bounds if null then entire dataset should be returned
	 * @return
	 * @throws IOException
	 */
	SimpleFeatureReader getWaterbodies(ReferencedEnvelope bounds) throws IOException;
	
	/**
	 * 
	 * @param bounds if null then entire dataset should be returned
	 * @return
	 * @throws IOException
	 */
	SimpleFeatureReader getFlowpaths(ReferencedEnvelope bounds) throws IOException;
	
	/**
	 * Get the bounds of the catchments dataset
	 * @return
	 * @throws IOException
	 */
	ReferencedEnvelope getCatchmentBounds() throws IOException;
}
