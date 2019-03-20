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
package net.refractions.chyf;

import java.util.List;
import java.util.UUID;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.WKTReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import net.refractions.chyf.enumTypes.CatchmentType;
import net.refractions.chyf.enumTypes.FlowpathType;
import net.refractions.chyf.enumTypes.Rank;
import net.refractions.chyf.hygraph.HyGraphBuilder;
import net.refractions.util.UuidUtil;
import nrcan.cccmeo.chyf.db.Catchment;
import nrcan.cccmeo.chyf.db.CatchmentDAO;
import nrcan.cccmeo.chyf.db.Flowpath;
import nrcan.cccmeo.chyf.db.FlowpathDAO;
import nrcan.cccmeo.chyf.db.SpringJdbcConfiguration;
import nrcan.cccmeo.chyf.db.Waterbody;
import nrcan.cccmeo.chyf.db.WaterbodyDAO;

/**
 * Reads source data from database.
 * 
 * @author Emily
 *
 */
//TODO: this reader needs to be updated to read the boundaries from the
//postgresql database - see the end of the read function.
public class ChyfPostgresqlReader extends ChyfDataReader{

	public ChyfPostgresqlReader() {
		
	}
	
	public void read(HyGraphBuilder gb ) throws Exception{
	
		try {
			GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(1000), ChyfDatastore.BASE_SRS);
			
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
			        gb.addECatchment(type, ((Polygon)waterCatchment).getArea(), (Polygon)waterCatchment);
			    }

			// read and add Catchments
			logger.info("Reading catchments");
			CatchmentDAO catchmentDAO = (CatchmentDAO) context.getBean(CatchmentDAO.class);
			
			List<Catchment> catchments = catchmentDAO.getCatchments(); 
			for (Catchment c : catchments) {
				Geometry catchment = GEOMETRY_FACTORY.createGeometry(wktreader.read(c.getLinestring()));
			    gb.addECatchment(CatchmentType.UNKNOWN, ((Polygon)catchment).getArea(), (Polygon)catchment);
			    }

			// read and add Flowpaths
			logger.info("Reading flowpaths");
			FlowpathDAO flow = (FlowpathDAO) context.getBean(FlowpathDAO.class);
			
			List<Flowpath> flowpaths = flow.getFlowpaths();
			for (Flowpath fp : flowpaths){
				Geometry flowPath = GEOMETRY_FACTORY.createGeometry(wktreader.read(fp.getLinestring()));
				FlowpathType type = FlowpathType.convert(fp.getType()); //ML
				String rankString = fp.getRank(); //ML
			    Rank rank = Rank.convert(rankString);
			    String name = fp.getName().intern(); //ML
			    UUID nameId = null;
			    try {
			    	nameId = UuidUtil.UuidFromString(fp.getNameId()); //ML
			    } catch(IllegalArgumentException iae) {
			    	logger.warn("Exception reading UUID: " + iae.getMessage());
			    }
			    gb.addEFlowpath(type, rank, name, nameId, ((LineString)flowPath).getLength(), (LineString)flowPath);
				
			}
			
			//TODO: the boundaries arraylist here should be updated to read the
			//boundary information from the database.  This is necessary to 
			//correctly assign hack order to the stream edges.  Without this
			//the hack order will be incorrect.  This array list
			//should be a list of geometries representing the boundary
			//of the dataset.  The assumption is that the boundary geometries
			//have exact same coordinate as the flowline where
			//the boundary meets the flowline
			
			//example: this.boundaries.add(e)
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
