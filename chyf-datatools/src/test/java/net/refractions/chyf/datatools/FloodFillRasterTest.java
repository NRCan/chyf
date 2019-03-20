package net.refractions.chyf.datatools;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.util.ArrayList;
import java.util.List;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;

public class FloodFillRasterTest {

	public static void main(String args[]) throws Exception {
		long now = System.nanoTime();
		String poly = "POLYGON (( 530.9 137, 388.6 178.3, 427.8 343.3, 337.1 427.8, 130.9 493.8, 54.6 728.9, 238.1 854.7, 568.1 788.7, 588.7 665, 411.3 619.6, 522.7 549.5, 757.8 543.3, 821.7 444.3, 799 264.9, 735.1 151.5, 530.9 137 ))";
		
		String ls = "LINESTRING ( 286.5 693.4, 280.5 633.5, 303 594.6, 358.4 584.1, 421.3 516.7, 530.6 447.8, 642.9 316, 650.4 206.7, 735.1 151.5 )";

		int cellsize = 10;
		int width = 100;
		
		BufferedImage polyimage = new BufferedImage(width,width,BufferedImage.TYPE_BYTE_GRAY);
        Graphics graphics = polyimage.getGraphics();
        
        GridGeometry2D gridGeom = new GridGeometry2D(new GridEnvelope2D(0, 0, width, width), (org.opengis.geometry.Envelope)new Envelope2D(null,0,0,cellsize*width,cellsize*width));
        DirectPosition2D worldPos = new DirectPosition2D();
        
        Polygon p = (Polygon) (new WKTReader()).read(poly);
        int[] coordGridX = new int[p.getExteriorRing().getNumPoints()];
        int[] coordGridY = new int[p.getExteriorRing().getNumPoints()];
        for (int n = 0; n < p.getExteriorRing().getNumPoints(); n++) {
        	worldPos.setLocation(p.getExteriorRing().getCoordinateN(n).x, p.getExteriorRing().getCoordinateN(n).y);
            GridCoordinates2D gridPos = gridGeom.worldToGrid(worldPos);
            coordGridX[n] = gridPos.x;
            coordGridY[n] = gridPos.y;
        }
        graphics.setColor(new Color(155,155,155)); //29
        graphics.drawPolyline(coordGridX, coordGridY, coordGridX.length);
  
        BufferedImage lineimsage = new BufferedImage(width,width,BufferedImage.TYPE_BYTE_GRAY);
        Graphics graphics2 = lineimsage.getGraphics();
        
        LineString l = (LineString)(new WKTReader()).read(ls);
        coordGridX = new int[l.getNumPoints()];
        coordGridY = new int[l.getNumPoints()];
        for (int n = 0; n < l.getNumPoints(); n++) {
            worldPos.setLocation(l.getCoordinateN(n).x, l.getCoordinateN(n).y);
            GridCoordinates2D gridPos = gridGeom.worldToGrid(worldPos);
            coordGridX[n] = gridPos.x;
            coordGridY[n] = gridPos.y;
        }
        graphics2.setColor(new Color(2,2,2)); //59
        graphics2.drawPolyline(coordGridX, coordGridY, coordGridX.length);
        
        
        DataBuffer buff = polyimage.getRaster().getDataBuffer();
        DataBuffer buff2 = lineimsage.getRaster().getDataBuffer();
        
        List<Point> startPoints = new ArrayList<>();
        for (int x = 0; x < width; x ++) {
        	for (int y = 0; y < width; y ++) {
//        		System.out.println(x +":" + y + ":" + image.getAsBufferedImage().getData().getSample(x, y, 0) );
//        		if (buff.getElem(y * width + x) != 0 ) {
//        			Coordinate c1 = new Coordinate(x * cellsize, (cellsize*width) - y * cellsize);
//        			Coordinate c2 = new Coordinate(x * cellsize+cellsize, (cellsize*width)- y * cellsize);
//        			Coordinate c3 = new Coordinate(x * cellsize+cellsize, (cellsize*width) -y * cellsize-cellsize);
//        			Coordinate c4 = new Coordinate(x * cellsize, (cellsize*width)-y * cellsize-cellsize);
//        			System.out.println( (new GeometryFactory()).createPolygon(new Coordinate[] {c1,c2,c3,c4,c1}).toText() );
//        		}
        		if (buff2.getElem(y * width + x) != 0) {
//        			System.out.println("POINT(" + (x*cellsize + 0.5*cellsize) + " " +((cellsize*width) -  (y*cellsize + 0.5*cellsize)) + ")");
        			startPoints.add(new Point(x,y));
//        			Coordinate c1 = new Coordinate(x * cellsize, (cellsize*width) - y * cellsize);
//        			Coordinate c2 = new Coordinate(x * cellsize+cellsize, (cellsize*width)- y * cellsize);
//        			Coordinate c3 = new Coordinate(x * cellsize+cellsize, (cellsize*width) -y * cellsize-cellsize);
//        			Coordinate c4 = new Coordinate(x * cellsize, (cellsize*width)-y * cellsize-cellsize);
//        			System.out.println( (new GeometryFactory()).createPolygon(new Coordinate[] {c1,c2,c3,c4,c1}).toText() );
        		}
//        		System.out.println(v);
//        			startPoints.add(new Point(x,y));
//        			System.out.println("POINT(" + x + " " + y + ")");
//        		}
        	}
        }
        
        graphics.dispose();
        
        int[][] data = new int[width][width];
        for (int i = 0; i < data.length; i ++) {
        	for (int j = 0; j < data.length; j ++) {
        		data[i][j] = -9999;
        	}
        }
        
        for (Point item : startPoints) {
        	data[item.x][item.y] = 0;
        }
        
        while(!startPoints.isEmpty()) {
        	Point item = startPoints.remove(0);
        	int value = data[item.x][item.y];
        	if (value == -9999) {
        		data[item.x][item.y] = 0;
        		value = 0;
        	}
        	boolean isStop = buff.getElem(item.y * width + item.x) != 0;
        	if (isStop) continue;
        	
        	if (item.x + 1 < data.length) {
        		int temp = data[item.x+1][item.y];
        		if (temp == -9999) {
        			data[item.x+1][item.y] = value + cellsize;
        			startPoints.add(new Point(item.x+1, item.y));
        		}else  if (value + cellsize < temp ) {
        			data[item.x+1][item.y] = value + cellsize;
        			startPoints.add(new Point(item.x+1, item.y));
        		}
        	}
        	if (item.x -1 >= 0) {
        		int temp = data[item.x-1][item.y];
        		if (temp == -9999) {
        			data[item.x-1][item.y] = value + cellsize;
        			startPoints.add(new Point(item.x-1, item.y));
        		}else  if (value + cellsize < temp ) {
        			data[item.x-1][item.y] = value + cellsize;
        			startPoints.add(new Point(item.x-1, item.y));
        		}
        	}
        	if (item.y + 1 < data.length) {
        		int temp = data[item.x][item.y+1];
        		if (temp == -9999) {
        			data[item.x][item.y+1] = value + cellsize;
        			startPoints.add(new Point(item.x, item.y+1));
        		}else  if (value + cellsize < temp ) {
        			data[item.x][item.y+1] = value + cellsize;
        			startPoints.add(new Point(item.x, item.y+1));
        		}
        	}
        	if (item.y -1 >= 0) {
        		int temp = data[item.x][item.y-1];
        		if (temp == -9999) {
        			data[item.x][item.y-1] = value + cellsize;
        			startPoints.add(new Point(item.x, item.y-1));
        		}else  if (value + cellsize < temp ) {
        			data[item.x][item.y-1] = value + cellsize;
        			startPoints.add(new Point(item.x, item.y-1));
        		}
        	}
        }
        
        for (int i = 0; i < data.length; i ++) {
        	for (int j = 0; j < data.length; j ++) {
        		System.out.print(data[j][i] + " ");
        	}
        	System.out.println();
        }
        
        
//        for (int x = 0; x < image.getWidth(); x ++) {
//        	for (int y = 0; y < image.getHeight(); y ++) {
////        		System.out.println(x +":" + y + ":" + image.getAsBufferedImage().getData().getSample(x, y, 0) );
//        		int v = buff.getElem(y * width + x);
//        		if (v  == -9999) {
//        			Coordinate c1 = new Coordinate(x * cellsize, (cellsize*width) - y * cellsize);
//        			Coordinate c2 = new Coordinate(x * cellsize+cellsize, (cellsize*width)- y * cellsize);
//        			Coordinate c3 = new Coordinate(x * cellsize+cellsize, (cellsize*width) -y * cellsize-cellsize);
//        			Coordinate c4 = new Coordinate(x * cellsize, (cellsize*width)-y * cellsize-cellsize);
//        			System.out.println( (new GeometryFactory()).createPolygon(new Coordinate[] {c1,c2,c3,c4,c1}).toText() );
//        			
//        		}
//        	}
//        }
//        for (int i = 0; i < image.getHeight() * image.getWidth();  i++) {
//        	System.out.println(i +":" + image.getAsBufferedImage().getData().getDataBuffer().getElem(i));
//        }
        
        long now2 = System.nanoTime();
        System.out.println(now2 - now);
	}
}
