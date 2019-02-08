package net.refractions.chyf.datatools.processor;

import java.util.List;

import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import net.refractions.chyf.datatools.readers.ChyfDataSource;

public class Distance2DProcessor {

	private CoordinateReferenceSystem toWork;
	private GeometryFactory gf = new GeometryFactory();
	
	public Distance2DProcessor(CoordinateReferenceSystem crs) {
		this.toWork = crs;
	}
	
	public void doWork(ChyfDataSource dataSource) throws Exception {
		
		ReferencedEnvelope env = dataSource.getCatchmentBounds();
		
		ReferencedEnvelope fullBounds = ReprojectionUtils.reproject(env, toWork);
		
		//lets make a 1m grid out of this
		
		try(SimpleFeatureReader reader = dataSource.getECatchments(null)){
			while(reader.hasNext()) {
				
				
				
			}
		}
		
		
	}
	private double processFeature(Polygon polygon, List<LineString> waterEdges) {
		Envelope env = polygon.getEnvelopeInternal();
		
		int startx = (int)Math.floor( env.getMinX() );
		int starty = (int)Math.floor( env.getMinY() );
		
		int endx = (int)Math.ceil( env.getMaxX() );
		int endy = (int)Math.ceil( env.getMaxY() );
		
		PreparedPolygon pp = new PreparedPolygon(polygon);
		
		double distanceSum = 0;
		int count = 0;
		for (int x = startx; x <= endx; x ++) {
			for (int y = starty; y <= endy; y ++) {
				Point p = gf.createPoint(new Coordinate(x,y));
				if (pp.contains(p)) {
					
					double d = Double.MAX_VALUE;
					for (LineString ls : waterEdges) {
						double d1 = DistanceOp.distance(p, ls);
						if (d1 < d) d = d1;
					}
					if (d != Double.MAX_VALUE) {
						distanceSum += d;
						count ++;
					}
				}
			}
			
		}
		if (count == 0) return Double.NaN;
		return distanceSum / count;
	}
}
