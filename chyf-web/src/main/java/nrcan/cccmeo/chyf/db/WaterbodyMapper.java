package nrcan.cccmeo.chyf.db;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class WaterbodyMapper implements RowMapper<Waterbody> {

	@Override
	public Waterbody mapRow(ResultSet resultSet, int i) throws SQLException {
		Waterbody waterbody = new Waterbody();
		waterbody.setDefinition(resultSet.getInt("definition"));
		waterbody.setArea(resultSet.getDouble("area"));
		waterbody.setLinestring(resultSet.getString("Linestring"));
		return waterbody;
	}

}
