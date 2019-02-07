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
