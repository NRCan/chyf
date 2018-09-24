package nrcan.cccmeo.benchmark;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

/**
 * This benchmark class is used for testing the controller of the REST API. 
 * The server must be started.
 */

public class BenchmarkController extends Benchmark {
	
	private static final String LOCAL_PATH = "http://localhost:8080/chyf-pilot";
	private static HttpHeaders headers;
	private static HttpEntity<String> entity;
	private static RestTemplate restTemplate;
	

	public BenchmarkController() {
		super();
		headers = new HttpHeaders();
		headers.set("Accept", "application/json");
		entity = new HttpEntity<String>(headers);
		restTemplate = new RestTemplate();
	}
	
	public static void main(String [] args) {
		new BenchmarkController();
		run(() -> tests());
	}
	
	private static void tests() {	
		warm(10, () -> getDrainageAreaDownstreamOfLocation());
		iterate(10, () -> getDrainageAreaDownstreamOfLocation(), "GetDrainageAreaDownstreamOfLocation");
		
		warm(10, () -> getDrainageAreaUpstreamOfLocation());
		iterate(10, () -> getDrainageAreaUpstreamOfLocation(), "GetDrainageAreaUpstreamOfLocation");
	}
	
	private static void getDrainageAreaDownstreamOfLocation() {
		
		restTemplate.exchange(LOCAL_PATH + "/drainageArea/downstreamOf.json?point=-73.35734367370607,44.97627451373233&removeHoles=false",
				HttpMethod.GET, entity, String.class);
	}
	
	private static void getDrainageAreaUpstreamOfLocation() {
			
		restTemplate.exchange(LOCAL_PATH + "/drainageArea/upstreamOf.json?point=-73.11985015869142,46.04715906440114&removeHoles=false",
				HttpMethod.GET, entity, String.class);
	}
	
}
