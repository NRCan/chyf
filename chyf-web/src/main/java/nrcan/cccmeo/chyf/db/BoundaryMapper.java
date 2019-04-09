package nrcan.cccmeo.chyf.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class BoundaryMapper implements RowMapper<Boundary> {

	@Override
	public Boundary mapRow(ResultSet resultSet, int i) throws SQLException {
		Boundary boundary = new Boundary();
		boundary.setLinestring(resultSet.getString("Linestring"));
		return boundary;
	}

}
