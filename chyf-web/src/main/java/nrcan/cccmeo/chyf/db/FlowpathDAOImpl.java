package nrcan.cccmeo.chyf.db;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FlowpathDAOImpl implements FlowpathDAO {
	
	private JdbcTemplate jdbcTemplate;
	private String query;
	
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }
	
	public void setSqlQuery(String query){
        this.query = query;
    }
	
	public List<Flowpath> getFlowpaths() {
		return (List<Flowpath>) jdbcTemplate.query(query, new FlowpathMapper());
	}

}
