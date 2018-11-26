package net.refractions.util;

public class StopWatch {
	
	private long startTime = 0;
	private long stopTime = 0;
	private boolean running = false;
	
	public void start() {
		this.startTime = System.nanoTime();
		this.running = true;
	}
	
	public void stop() {
		this.stopTime = System.nanoTime();
		this.running = false;
	}
	
	/**
	 * Returns elapsed time in milliseconds
	 * @return elapsed time in milliseconds
	 */
	public double getElapsedTime() {
		double elapsed;
		if(running) {
			elapsed = (System.nanoTime() - startTime) / 1000000.0;
		}
		else {
			elapsed = (stopTime - startTime) / 1000000.0;
		}
		return elapsed;
	}
	
	/**
	 * Returns elapsed time in seconds
	 * @return elapsed time in seconds
	 */
	public double getElapsedTimeSecs() {
		return getElapsedTime() / 1000.0;
	}
	
	// sample usage
	public static void main(String[] args) {
		StopWatch s = new StopWatch();
		s.start();
		// code you want to time goes here
		s.stop();
		System.out.println("elapsed time in milliseconds: " + s.getElapsedTime());
	}
}
