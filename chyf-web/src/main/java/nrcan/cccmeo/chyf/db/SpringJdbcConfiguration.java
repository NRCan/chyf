package nrcan.cccmeo.chyf.db;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

//Inspired by http://www.topjavatutorial.com/frameworks/spring/spring-jdbc/spring-jdbc-using-annotation-based-configuration/

@Configuration
@ComponentScan("nrcan.ccmeo.chyf.db")
@PropertySources({@PropertySource("classpath:database.properties"), 
				  @PropertySource("classpath:queries.properties")})
public class SpringJdbcConfiguration {
	
	@Autowired
	Environment env;

	private final String URL = "url";
	private final String USER = "dbuser";
	private final String DRIVER = "driver";
	private final String PASSWORD = "dbpassword";
	
	@Bean
	public DataSource dataSource(){
		DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();
		driverManagerDataSource.setDriverClassName(env.getProperty(DRIVER));
		driverManagerDataSource.setUrl(env.getProperty(URL));
		driverManagerDataSource.setUsername(env.getProperty(USER));
		driverManagerDataSource.setPassword(env.getProperty(PASSWORD));
		return driverManagerDataSource;
	}
	
	@Bean
	public JdbcTemplate jdbcTemplate() {
		JdbcTemplate jdbcTemplate = new JdbcTemplate();
		jdbcTemplate.setDataSource(dataSource());
		jdbcTemplate.setResultsMapCaseInsensitive(true);
		return jdbcTemplate;
	}
	
	@Bean
	public FlowpathDAO flowpathDAO() {
		FlowpathDAOImpl flowDAO = new FlowpathDAOImpl();
		flowDAO.setJdbcTemplate(jdbcTemplate());
		flowDAO.setSqlQuery(env.getProperty("sql.select.flowpath"));
		return flowDAO;
	}
	
	@Bean
	public CatchmentDAO catchmentDAO() {
		CatchmentDAOImpl catchDAO = new CatchmentDAOImpl();
		catchDAO.setJdbcTemplate(jdbcTemplate());
		catchDAO.setSqlQuery(env.getProperty("sql.select.catchment"));
		return catchDAO;
	}
	
	@Bean
	public WaterbodyDAO waterbodyDAO() {
		WaterbodyDAOImpl waterDAO = new WaterbodyDAOImpl();
		waterDAO.setJdbcTemplate(jdbcTemplate());
		waterDAO.setSqlQuery(env.getProperty("sql.select.waterbody"));
		return waterDAO;
	}
	
	@Bean
	public BoundaryDAO boundaryDAO() {
		BoundaryDAOImpl boundaryDAO = new BoundaryDAOImpl();
		boundaryDAO.setJdbcTemplate(jdbcTemplate());
		boundaryDAO.setSqlQuery(env.getProperty("sql.select.boundary"));
		return boundaryDAO;
	}
}
