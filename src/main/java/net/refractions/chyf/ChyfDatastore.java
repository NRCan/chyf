package net.refractions.chyf;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import net.refractions.chyf.enumTypes.FlowpathRank;
import net.refractions.chyf.enumTypes.FlowpathType;
import net.refractions.chyf.hydrograph.EFlowpath;
import net.refractions.chyf.hydrograph.HyGraph;
import net.refractions.chyf.hydrograph.HyGraphBuilder;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class ChyfDatastore {
	
	private HyGraph hyGraph;

	public ChyfDatastore(String dataDir) {
		init(dataDir);
	}

	private void init(String dataDir) {
		try {
			HyGraphBuilder gb = new HyGraphBuilder();
			DataStore dataStore = getShapeFileDataStore(dataDir + "Flowpath.shp");
		    String typeName = dataStore.getTypeNames()[0];
		    FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
		    FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures();
		    try (FeatureIterator<SimpleFeature> features = collection.features()) {
		        while (features.hasNext()) {
		            SimpleFeature feature = features.next();
		            //System.out.print(feature.getID());
		            LineString flowPath = (LineString)(((MultiLineString)(feature.getDefaultGeometryProperty().getValue())).getGeometryN(0));
		            FlowpathType type = FlowpathType.convert((String)feature.getAttribute("type"));
		            FlowpathRank rank = FlowpathRank.convert((String)feature.getAttribute("rank"));
		            gb.addEFlowpath(type, rank, flowPath);
		        }
		    }
		    dataStore.dispose();
		    hyGraph = gb.build();
		} catch(IOException e) {
			// System.out.println(e.getMessage());
			throw new RuntimeException(e);
		}
	}
	
	private DataStore getShapeFileDataStore(String fileName) throws IOException {
	    File file = new File(fileName);
	    Map<String, Object> map = new HashMap<>();
		map.put("url", file.toURI().toURL());
		map.put("charset", Charset.forName("ISO-8859-1"));
	    return DataStoreFinder.getDataStore(map);
	}
	
	public EFlowpath getEdge(int id) {
		return hyGraph.getEFlowpath(id);
	}
	
}
