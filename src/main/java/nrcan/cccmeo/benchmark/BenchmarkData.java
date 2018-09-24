package nrcan.cccmeo.benchmark;

import java.util.List;

import com.vividsolutions.jts.io.ParseException;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.hygraph.HyGraphBuilder;
import net.refractions.chyf.hygraph.StreamOrderCalculator;
import nrcan.cccmeo.chyf.db.Catchment;
import nrcan.cccmeo.chyf.db.Flowpath;
import nrcan.cccmeo.chyf.db.Waterbody;

/**
 * This benchmark class is used for testing the creation of the chyf application. 
 * The server does not have to be started.
 */

public class BenchmarkData extends Benchmark {
	
	private static ChyfDatastore chyf;
	private static HyGraphBuilder gb;
	private static List<Waterbody> waterbodies;
	private static List<Catchment> catchments;
	private static List<Flowpath> flowpaths;
	
	public BenchmarkData() {
		super();
		chyf = new ChyfDatastore(true);
		gb = new HyGraphBuilder();
	}
	
	
	public static void main(String [] args) {
		new BenchmarkData();
		run(() -> tests());
	}
	
	private static void tests() {
		warm(10, () -> readWaterbodies());
		iterate(10, () -> readWaterbodies(), "ReadWaterBodies");
		warm(10, () -> createWaterbodies());
		iterate(10, () -> createWaterbodies(), "CreateWaterBodies");
	
		warm(10, () -> readCatchments());
		iterate(10, () -> readCatchments(), "ReadCatchments");
		warm(10, () -> createCatchments());
		iterate(10, () -> createCatchments(), "CreateCatchments");
		
		warm(10, () -> readFlowpaths());
		iterate(10, () -> readFlowpaths(), "ReadFlowpaths");
		warm(10, () -> createFlowpaths());
		iterate(10, () -> createFlowpaths(), "CreateFlowpaths");
		
		warm(10, () -> streamOrderCalculator());
		iterate(10, () -> streamOrderCalculator(), "StreamOrderCalculator");
	}
	
	
	private static void readWaterbodies() {
		waterbodies = chyf.read(Waterbody.class);
	}
	
	private static void createWaterbodies() {
		try {
			gb.clearCatchments();
			chyf.createWaterbodies(waterbodies, gb);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	private static void readCatchments() {
		catchments = chyf.read(Catchment.class);
	}
	
	private static void createCatchments() {
		try {
			gb.clearCatchments();
			chyf.createCatchments(catchments, gb);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	private static void readFlowpaths() {
		flowpaths = chyf.read(Flowpath.class);
	}
	
	private static void createFlowpaths() {
		try {
			gb.clearFlowpaths();
			chyf.createFlowpaths(flowpaths, gb);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	private static void streamOrderCalculator() {
		StreamOrderCalculator.calcOrders(gb.getEFlowpaths(), gb.getNexuses());
	}

}
