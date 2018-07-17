package nrcan.cccmeo.chyf.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class CatchmentMapper implements RowMapper<Catchment> {
	
	@Override
	public Catchment mapRow(ResultSet resultSet, int i) throws SQLException {
		Catchment catchment = new Catchment();
		catchment.setLinestring(resultSet.getString("Linestring"));
		return catchment;
	}

}
