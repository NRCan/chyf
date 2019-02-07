package net.refractions.chyf.datatools.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;

import net.refractions.chyf.datatools.readers.ChyfDataSource;
import net.refractions.chyf.datatools.readers.GeoTiffDemReader;

/**
 * Computes slope/aspect/elevation statistics for catchments
 * @author Emily
 *
 */
public class SEAProcessor {

	private int processingtilesize = 1024; 
	
	private ChyfDataSource data;
	private GeoTiffDemReader dem;
	
	private List<Tile> tiles;
	
	public SEAProcessor (ChyfDataSource data, GeoTiffDemReader dem) throws IOException {
		this.data = data;
		this.dem = dem;
		this.tiles = generateTiles();
	}
	
	
	public SEAResult doWork() throws Exception {
		//process by tile
		List<SEAResult> results = new ArrayList<>();
		
		int cnt = 0;
		for (Tile tile : tiles) {
			cnt ++;
			System.out.println(cnt + "/" + tiles.size());
			results.add( (new SEATileProcessor(dem, data)).doWork(tile) );
		}
		
		//merge all results
		SEAResult total = new SEAResult();
		results.forEach(r->total.merge(r));
		return total;
	}
	
	private List<Tile> generateTiles() throws IOException {
		GeneralEnvelope genv = dem.getFileBounds();
		ReferencedEnvelope env = new ReferencedEnvelope(genv.getMinimum(0), genv.getMaximum(0), genv.getMinimum(1), genv.getMaximum(1), dem.getCrs());
		
		int numx = (int) Math.ceil( env.getWidth() / processingtilesize );
		int numy = (int) Math.ceil( env.getHeight() / processingtilesize );
		
		List<Tile> toProcess = new ArrayList<>();
		
		for (int x = 0; x < numx; x ++) {
			for (int y = 0; y < numy; y ++) {
				double x1 = x * processingtilesize + env.getMinX();
				double y1 = y * processingtilesize + env.getMinY();
				double x2 = (x+1) * processingtilesize + env.getMinX();
				double y2 = (y+1) * processingtilesize + env.getMinY();
				ReferencedEnvelope re = new ReferencedEnvelope(x1, x2, y1, y2, env.getCoordinateReferenceSystem());
				
				ReprojectionUtils.reproject(re, env.getCoordinateReferenceSystem());
				
				toProcess.add(new Tile(re));
			}
		}
		return toProcess;
	}
}
