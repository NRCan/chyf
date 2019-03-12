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
	public long getElapsedTime() {
		long elapsed;
		if(running) {
			elapsed = (System.nanoTime() - startTime) / 1000000;
		}
		else {
			elapsed = (stopTime - startTime) / 1000000;
		}
		return elapsed;
	}
	
	/**
	 * Returns elapsed time in seconds
	 * @return elapsed time in seconds
	 */
	public long getElapsedTimeSecs() {
		return getElapsedTime() / 1000;
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
