package nrcan.cccmeo.chyf.db;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;

public class CatchmentDAOImpl implements CatchmentDAO {
	
	private JdbcTemplate jdbcTemplate;
	private String query;
	
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }
	
	public void setSqlQuery(String query){
		this.query = query;
	}

	@Override
	public List<Catchment> getCatchments() {
		return (List<Catchment>) jdbcTemplate.query(query, new CatchmentMapper());
	}

}
