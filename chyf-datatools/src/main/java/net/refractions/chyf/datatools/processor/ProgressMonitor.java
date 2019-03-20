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
package net.refractions.chyf.datatools.processor;

public class ProgressMonitor {

	private int taskLength;
	private int worked;
	
	public ProgressMonitor() {
		this.taskLength = 0;
		this.worked = 0;
	}
	
	public void setTaskLength(int length) {
		this.taskLength = length;
	}
	
	public void worked(int amount) {
		worked+=amount;
	}
	
	public int getPercentage() {
		if (taskLength == 0) return 0;
		return (int) (Math.round((worked / (double)taskLength) * 100));
	}
	
}
