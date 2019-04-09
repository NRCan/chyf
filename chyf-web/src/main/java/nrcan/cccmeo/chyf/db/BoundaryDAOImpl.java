package nrcan.cccmeo.chyf.db;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

public class BoundaryDAOImpl implements BoundaryDAO {
	
	private JdbcTemplate jdbcTemplate;
	private String query;
	
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }
	
	public void setSqlQuery(String query){
        this.query = query;
    }
	
	public List<Boundary> getBoundary() {
		return (List<Boundary>) jdbcTemplate.query(query, new BoundaryMapper());
	}
	
}
