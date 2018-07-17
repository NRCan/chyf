package net.refractions.chyf;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.refractions.chyf.enumTypes.CatchmentType;
import net.refractions.chyf.enumTypes.FlowpathType;
import net.refractions.chyf.hygraph.HyGraph;
import net.refractions.chyf.hygraph.HyGraphBuilder;
import net.refractions.chyf.rest.GeotoolsGeometryReprojector;
import net.refractions.util.UuidUtil;
import nrcan.cccmeo.chyf.db.SpringJdbcConfiguration;
import nrcan.cccmeo.chyf.db.Waterbody;
import nrcan.cccmeo.chyf.db.WaterbodyDAO;
import nrcan.cccmeo.chyf.db.Catchment;
import nrcan.cccmeo.chyf.db.CatchmentDAO;
import nrcan.cccmeo.chyf.db.Flowpath;
import nrcan.cccmeo.chyf.db.FlowpathDAO;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.Hints;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKTReader;

public class ChyfDatastore {
	static final Logger logger = LoggerFactory.getLogger(ChyfDatastore.class.getCanonicalName());
	
	public static final int BASE_SRS = 6624; // Quebec Albers
	public static GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), BASE_SRS);
	public static final int MAX_RESULTS = 20000;
	
	private HyGraph hyGraph;

	public ChyfDatastore() {
		init();
	}
	
	public ChyfDatastore(String dataDir) {
		init(dataDir);
	}

	public HyGraph getHyGraph() {
		return hyGraph;
	}
	
	private void init() {
		try {
			GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(1000), BASE_SRS);
			
		    HyGraphBuilder gb = new HyGraphBuilder();
		    
			@SuppressWarnings("resource")
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SpringJdbcConfiguration.class); //ML
			WKTReader wktreader = new WKTReader();

			// read and add Waterbodies
			logger.info("Reading waterbodies");
		    WaterbodyDAO waterbodyDAO = (WaterbodyDAO) context.getBean(WaterbodyDAO.class);
			    
		    for(Waterbody wb : waterbodyDAO.getWaterbodies()) {
		    	Geometry waterCatchment = GEOMETRY_FACTORY.createGeometry(wktreader.read(wb.getLinestring()));
			    CatchmentType type = CatchmentType.UNKNOWN;
			    switch(wb.getDefinition()) {
			        case 1:
			            type = CatchmentType.WATER_CANAL;
			            break;
			        case 4:
			           	type = CatchmentType.WATER_LAKE;
			           	break;
			        case 6: 
			           	type = CatchmentType.WATER_RIVER;
			           	break;
			        case 9:
			           	type = CatchmentType.WATER_POND;
			           	break;
			        }
			        gb.addECatchment(type, (Polygon)waterCatchment);
			    }

			// read and add Catchments
			logger.info("Reading catchments");
			CatchmentDAO catchmentDAO = (CatchmentDAO) context.getBean(CatchmentDAO.class);
			
			List<Catchment> catchments = catchmentDAO.getCatchments(); 
			for (Catchment c : catchments) {
				Geometry catchment = GEOMETRY_FACTORY.createGeometry(wktreader.read(c.getLinestring()));
			    gb.addECatchment(CatchmentType.UNKNOWN, (Polygon)catchment);
			    }

			// read and add Flowpaths
			logger.info("Reading flowpaths");
			FlowpathDAO flow = (FlowpathDAO) context.getBean(FlowpathDAO.class);
			
			List<Flowpath> flowpaths = flow.getFlowpaths();
			for (Flowpath fp : flowpaths){
				Geometry flowPath = GEOMETRY_FACTORY.createGeometry(wktreader.read(fp.getLinestring()));
				FlowpathType type = FlowpathType.convert(fp.getType()); //ML
				String rankString = fp.getRank(); //ML
			    int rank = -1;
			    if(rankString.equals("Primary")) {
			    	rank = 1;
			    } else if(rankString.equals("Secondary")) {
			    	rank = 2;
			    } 
				String name = fp.getName().intern(); //ML
			    UUID nameId = null;
			    try {
			    	nameId = UuidUtil.UuidFromString(fp.getNameId()); //ML
			    } catch(IllegalArgumentException iae) {
			    	logger.warn("Exception reading UUID: " + iae.getMessage());
			    }
			    Integer certainty = fp.getCertainty();
			    gb.addEFlowpath(type, rank, name, nameId, certainty, (LineString)flowPath);
				
			}
			
			hyGraph = gb.build();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void init(String dataDir) {
		try {
			// the shapefile data is actually in 4617 CSRS/GRS80/NAD83
			// but we are going to reproject it into BASE_SRS
			// so this tricks it into using a geometry factory with the BASE_SRS 
			Hints hints = new Hints(Hints.JTS_GEOMETRY_FACTORY, GEOMETRY_FACTORY);
		    Query query = new Query();
		    query.setHints(hints);

		    HyGraphBuilder gb = new HyGraphBuilder();

			// read and add Waterbodies
		    logger.info("Reading waterbodies shapefile");
			DataStore waterbodyDataStore = getShapeFileDataStore(dataDir + "Waterbody.shp");
		    String waterbodyTypeName = waterbodyDataStore.getTypeNames()[0];
		    FeatureSource<SimpleFeatureType, SimpleFeature> waterbodyFeatureSource = waterbodyDataStore.getFeatureSource(waterbodyTypeName);
		    FeatureCollection<SimpleFeatureType, SimpleFeature> waterbodyFeatureCollection = waterbodyFeatureSource.getFeatures(query);
		    try (FeatureIterator<SimpleFeature> features = waterbodyFeatureCollection.features()) {
		        while (features.hasNext()) {
		            SimpleFeature feature = features.next();
		            //System.out.print(feature.getID());
		            Polygon catchment = (Polygon)(((MultiPolygon)(feature.getDefaultGeometryProperty().getValue())).getGeometryN(0));
		            catchment.setSRID(4617); 
		            catchment = GeotoolsGeometryReprojector.reproject(catchment, BASE_SRS);
		            CatchmentType type = CatchmentType.UNKNOWN;
		            switch(((Long)feature.getAttribute("DEFINITION")).intValue()) {
		            case 1:
		            	type = CatchmentType.WATER_CANAL;
		            	break;
		            case 4:
		            	type = CatchmentType.WATER_LAKE;
		            	break;
		            case 6: 
		            	type = CatchmentType.WATER_RIVER;
		            	break;
		            case 9:
		            	type = CatchmentType.WATER_POND;
		            	break;
		            }
		            gb.addECatchment(type, catchment);
		        }
		    }
		    waterbodyDataStore.dispose();

			// read and add Catchments
		    logger.info("Reading catchments shapefile");
			DataStore catchmentDataStore = getShapeFileDataStore(dataDir + "Catchment.shp");
		    String catchmentTypeName = catchmentDataStore.getTypeNames()[0];
		    FeatureSource<SimpleFeatureType, SimpleFeature> catchmentFeatureSource = catchmentDataStore.getFeatureSource(catchmentTypeName);
		    FeatureCollection<SimpleFeatureType, SimpleFeature> catchmentFeatureCollection = catchmentFeatureSource.getFeatures(query);
		    try (FeatureIterator<SimpleFeature> features = catchmentFeatureCollection.features()) {
		        while (features.hasNext()) {
		            SimpleFeature feature = features.next();
		            //System.out.print(feature.getID());
		            Polygon catchment = (Polygon)(((MultiPolygon)(feature.getDefaultGeometryProperty().getValue())).getGeometryN(0));
		            catchment.setSRID(4617); 
		            catchment = GeotoolsGeometryReprojector.reproject(catchment, BASE_SRS);
		            gb.addECatchment(CatchmentType.UNKNOWN, catchment);
		        }
		    }
		    catchmentDataStore.dispose();

		    // read and add Flowpaths
		    logger.info("Reading flowpaths shapefile");
			DataStore flowPathDataStore = getShapeFileDataStore(dataDir + "Flowpath.shp");
		    String flowPathTypeName = flowPathDataStore.getTypeNames()[0];
		    FeatureSource<SimpleFeatureType, SimpleFeature> flowpathFeatureSource = flowPathDataStore.getFeatureSource(flowPathTypeName);
		    FeatureCollection<SimpleFeatureType, SimpleFeature> flowpathFeatureCollection = flowpathFeatureSource.getFeatures(query);
		    try (FeatureIterator<SimpleFeature> features = flowpathFeatureCollection.features()) {
		        while (features.hasNext()) {
		            SimpleFeature feature = features.next();
		            //System.out.print(feature.getID());
		            LineString flowPath = (LineString)(((MultiLineString)(feature.getDefaultGeometryProperty().getValue())).getGeometryN(0));
		            flowPath.setSRID(4617); // CSRS/GRS80/NAD83/
		            flowPath = GeotoolsGeometryReprojector.reproject(flowPath, BASE_SRS);
		            FlowpathType type = FlowpathType.convert((String)feature.getAttribute("TYPE"));
		            String rankString = (String)feature.getAttribute("RANK");
		            int rank = -1;
		            if(rankString.equals("Primary")) {
		            	rank = 1;
		            } else if(rankString.equals("Secondary")) {
		            	rank = 2;
		            } 
		            String name = ((String)feature.getAttribute("NAME")).intern();
		            UUID nameId = null;
		            try {
		            	nameId = UuidUtil.UuidFromString((String)feature.getAttribute("NAMEID"));
		            } catch(IllegalArgumentException iae) {
		            	logger.warn("Exception reading UUID: " + iae.getMessage());
		            }
		            Integer certainty = (Integer)feature.getAttribute("DIRECTION");
		            gb.addEFlowpath(type, rank, name, nameId, certainty, flowPath);
		        }
		    }
		    flowPathDataStore.dispose();
		    
		    hyGraph = gb.build();
		} catch(IOException e) {
			// System.out.println(e.getMessage());
			throw new RuntimeException(e);
		}
	}
	
	private DataStore getShapeFileDataStore(String fileName) throws IOException {
	    File file = new File(fileName);
	    Map<String, Object> map = new HashMap<String, Object>();
		map.put("url", file.toURI().toURL());
		map.put("charset", Charset.forName("ISO-8859-1"));
	    return DataStoreFinder.getDataStore(map);
	}
}
