package net.refractions.chyf.datatools;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.operation.distance.DistanceOp;

public class GeometryRasterTest {

	public static void main(String args[]) throws Exception {
		long now = System.nanoTime();
		String polywt = "POLYGON (( 530.9 137, 388.6 178.3, 427.8 343.3, 337.1 427.8, 130.9 493.8, 54.6 728.9, 238.1 854.7, 568.1 788.7, 588.7 665, 411.3 619.6, 522.7 549.5, 757.8 543.3, 821.7 444.3, 799 264.9, 735.1 151.5, 530.9 137 ))";
		
		String lswt = "LINESTRING ( 286.5 693.4, 280.5 633.5, 303 594.6, 358.4 584.1, 421.3 516.7, 530.6 447.8, 642.9 316, 650.4 206.7, 735.1 151.5 )";

		int cellsize = 10;
		int width = 100;
		
		
		Polygon p = (Polygon) (new WKTReader()).read(polywt);
		PreparedPolygon pp = new PreparedPolygon(p);
		LineString ls = (LineString) (new WKTReader()).read(lswt);
		
		GeometryFactory gf = new GeometryFactory();
		double[][] data = new double[width][width];
		for (int x = 0; x < width; x ++) {
			for (int y = 0; y < width; y ++) {
				
				Coordinate c = new Coordinate(x*cellsize + cellsize / 2, y * cellsize + cellsize / 2);
				Point pnt = gf.createPoint(c);
				if (pp.contains(pnt)) {
					data[x][y] = DistanceOp.distance(ls, pnt);
				}else {
					data[x][y] = -9999;
				}
			}
		}
		System.out.println("ncols " + width);
		System.out.println("nrows " + width);
		System.out.println("xllcorner 0.0");
		System.out.println("yllcorner 0.0");
		System.out.println("cellsize " + cellsize);
		System.out.println("NODATA_value -9999");
		  for (int i = 0; i < data.length; i ++) {
	        	for (int j = 0; j < data.length; j ++) {
	        		System.out.print(data[j][data.length - 1 - i] + " ");
	        	}
	        	System.out.println();
	        }
		
		
        long now2 = System.nanoTime();
        System.out.println(now2 - now);
	}
}
