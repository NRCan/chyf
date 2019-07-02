package net.refractions.util;




import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

// A CHANGER DANS LE REPERTOIRE MESSAGE CONVERTER?
import org.json.*;
import org.json.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.refractions.chyf.hygraph.Coverage;


public class JsonConverter {
	
//Constructeur
 public JsonConverter() {
	
}

 //Extract informations content in the Json of the cube for the landcover 
 public ArrayList<Coverage> convertJsonToCover(String jsonString)
 {
	 ArrayList<Coverage>  coverageList = new ArrayList<Coverage>();	
	 JsonParser parser = new JsonParser(); 
	 JsonObject jObject = parser.parse(jsonString).getAsJsonObject(); 
	 
	 //Savoir les keys du json
	 Set<Map.Entry<String, JsonElement>> entries = jObject.entrySet();
	 for (Map.Entry<String, JsonElement> entry: entries)
	 {		
		 //si la key = coverage
		  if(entry.getKey().toString().equals("coverage"))
		  {
			 JsonElement jsonCoverage = entry.getValue();
			 JsonObject objCoverage = jsonCoverage.getAsJsonObject();

			 //avoir les key and values dans le json du coverage
			 Set<Map.Entry<String, JsonElement>> entries2 = objCoverage.entrySet();
			 for (Map.Entry<String, JsonElement> entry2: entries2)
			 {
				 Coverage coverage = new Coverage(Integer.parseInt(entry2.getKey()), Double.parseDouble(entry2.getValue().toString()));
				 coverageList.add(coverage);
			 }
		}
	 }
	 return coverageList;
 	}
 
}
