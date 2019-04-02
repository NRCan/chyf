package nrcan.cccmeo.chyf.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class FlowpathMapper implements RowMapper<Flowpath> {

	@Override
	public Flowpath mapRow(ResultSet resultSet, int i) throws SQLException {
		Flowpath flowpath = new Flowpath();
		flowpath.setName(resultSet.getString("name"));
		flowpath.setNameId(resultSet.getString("nameId"));
		flowpath.setType(resultSet.getString("type"));
		flowpath.setRank(resultSet.getString("rank"));
		flowpath.setLength(resultSet.getDouble("length"));
		flowpath.setLinestring(resultSet.getString("Linestring"));
		return flowpath;
	}

}
