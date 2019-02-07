package nrcan.cccmeo.chyf.db;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

public class WaterbodyDAOImpl implements WaterbodyDAO {
	
	private JdbcTemplate jdbcTemplate;
	private String query;
	
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }
	
	public void setSqlQuery(String query){
        this.query = query;
    }
	
	public List<Waterbody> getWaterbodies() {
		return (List<Waterbody>) jdbcTemplate.query(query, new WaterbodyMapper());
	}
	
}
