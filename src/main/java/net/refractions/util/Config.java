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
