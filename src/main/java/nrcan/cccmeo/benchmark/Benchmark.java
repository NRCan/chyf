package nrcan.cccmeo.benchmark;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Formatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.refractions.util.StopWatch;


public abstract class Benchmark {
	
	protected static final Logger logger = LoggerFactory.getLogger(Benchmark.class.getCanonicalName());
	protected static final String UNIT = "s/op";
	protected static final int MAX_ITERATION = 100;
	
	protected static Formatter fmt;
	protected static double[] iterationsTime;
	protected static StopWatch stopWatch;

	public Benchmark() {
		stopWatch = new StopWatch();
		iterationsTime = new double[MAX_ITERATION];
		fmt = new Formatter();
	}
	
	/**
	 * The main method that executes every functions
	 * @param functions - The set of functions to execute 
	 */
	public static void run(Runnable functions) {
		
		logger.info("----------------------------------------------");
		logger.info("Start Benchmarking");
		
		LocalDateTime ldt = LocalDateTime.now();
		DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		logger.info(format.format(ldt));
		
		fmt.format("%-50s %10s %10s %10s\n", "Method", "iterations", "Score", "Units");
		logger.info(fmt.toString());
		
		functions.run();
		
		System.out.println("End Benchmarking");
		logger.info("----------------------------------------------");
	}
	
	/**
	 * Execute a function several times to warm up the JVM
	 * @param iterations - The number of times the function is executed
	 * @param function - The function to execute
	 */
	protected static void warm(int iterations, Runnable function) {
		for(Integer i = 0; i < iterations; i++) {
			function.run();
		}
	}
	
	/**
	 * Execute a single function multiple times and write the average result into a file
	 * @param iterations - The number of times the function is executed
	 * @param function - The function to execute
	 * @param nameFunction - The function's name
	 */
	protected static void iterate(int iterations, Runnable function, String nameFunction) {	
		for(Integer i = 0; i < iterations; i++) {
			stopWatch.start();
			function.run();
			stopWatch.stop();
			iterationsTime[i] = stopWatch.getElapsedTimeSecs();
		}		    
		double mean = getMean(iterationsTime, iterations);
		
		fmt = new Formatter();
		fmt.format("%-50s %10d %10g %10s\n", nameFunction, iterations, mean, UNIT);
		logger.info(fmt.toString());
	}
	
	/**
	 * Calculate the mean
	 * @param table - The number table
	 * @param nbElement - The number of elements
	 * @return - The mean
	 */
	private static double getMean(double[] table, int nbElement) {
		double sum = 0;
	    for (int i = 0; i < nbElement; i++) {
	        sum += table[i];
	    }
	    return sum / nbElement;
	}
	
}


