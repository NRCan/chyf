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

import java.util.Properties;

public class Config {
	
	private Properties configFile;
	private static Config instance;
	
	public Config(){
		configFile = new java.util.Properties();
		try{
			configFile.load(this.getClass().getClassLoader().getResourceAsStream("config.cfg"));
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	public String getProperty(String key){
		return this.configFile.getProperty(key);
	}

	public static Config getInstance() {
		return instance;
	}

	public static void setInstance(Config instance) {
		Config.instance = instance;
	}

}
