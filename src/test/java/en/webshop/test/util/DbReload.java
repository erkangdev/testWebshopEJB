package en.webshop.test.util;

import static org.dbunit.operation.DatabaseOperation.CLEAN_INSERT;


import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.inject.Named;
import javax.inject.Singleton;

import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.db2.Db2Connection;
import org.dbunit.ext.mssql.InsertIdentityOperation;
import org.dbunit.ext.mssql.MsSqlConnection;
import org.dbunit.ext.mysql.MySqlConnection;
import org.dbunit.ext.oracle.OracleConnection;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


@Named
@Singleton
public class DbReload {
	private static final Logger LOGGER = LoggerFactory.getLogger(DbReload.class);
	private static final String BEAN_NAME = getBeanName();
	
	private static boolean reloaded = false;
	
	@Value("${db.url:jdbc:postgresql:webshopdb}")
	private String url;
	
	@Value("${db.username:webshop}")
	private String username;
	
	@Value("${db.password}")
	private String password;
	
	@Value("${dbUnit.xmlDataset:db.xml}")
	private String xmlDataset;

	@Value("${dbUnit.schema:webshop}")
	private String schema;              // nicht benoetigt fuer MySQL
	
	
	/**
	 * Private Konstruktor genuegt fuer Spring
	 */
	private DbReload() {
	}
	
	/**
	 * @return Klassenname ermitteln: mit Kleinbuchstabe am Anfang, z.B. DbUnit -> dbUnit
	 */
	private static String getBeanName() {
		final String className = DbReload.class.getSimpleName();
		return "" + Character.toLowerCase(className.charAt(0)) + className.substring(1);
	}
	
	/**
	 * DB neu laden
	 */
	public static void reload() throws SQLException, DatabaseUnitException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
		if (reloaded == true)
			return;
			
		final ApplicationContext ctx = new ClassPathXmlApplicationContext("test.xml");
		final DbReload dbReload = (DbReload) ctx.getBean(BEAN_NAME);
		
		final Connection jdbcConn = DriverManager.getConnection(dbReload.url, dbReload.username, dbReload.password);
		IDatabaseConnection dbunitConn = null;
		boolean caseSensitiveTableNames = false;
		if (dbReload.url.contains("postgres")) {
			dbunitConn = new DatabaseConnection(jdbcConn, dbReload.schema);
			dbunitConn.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new PostgresqlDataTypeFactory());
			caseSensitiveTableNames = true;
		}
		else if (dbReload.url.contains("mysql")) {
			dbunitConn = new MySqlConnection(jdbcConn, null);
			if (System.getProperty("os.name").contains("Linux")) {
				caseSensitiveTableNames = true;
			}
		}
		else if (dbReload.url.contains("oracle")) {
			dbunitConn = new OracleConnection(jdbcConn, dbReload.schema);
		}
		else if (dbReload.url.contains("sqlserver")) {
			dbunitConn = new MsSqlConnection(jdbcConn, dbReload.schema);
		}
		else if (dbReload.url.contains("db2")) {
			dbunitConn = new Db2Connection(jdbcConn, dbReload.schema);
			if (System.getProperty("os.name").contains("Linux")) {
				caseSensitiveTableNames = true;
			}
		}
		else {
			throw new IllegalStateException("Die Datenbank-URL " + dbReload.url + " wird nicht unterstuetzt");
		}
		
		if (caseSensitiveTableNames) {
			dbunitConn.getConfig().setProperty(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, true);
		}
		
		final File xmlFile = new File(dbReload.xmlDataset);
		final FlatXmlDataSetBuilder flatXmlDataSetBuilder = new FlatXmlDataSetBuilder();
		flatXmlDataSetBuilder.setCaseSensitiveTableNames(caseSensitiveTableNames);
		final FlatXmlDataSet xmlDataset = flatXmlDataSetBuilder.build(xmlFile);
		final DatabaseOperation dbOp = dbReload.url.contains("sqlserver") ?
											// Fuer SQL Server ist ein spezieller INSERT-Modus notwendig,
											// damit IDENTITY-Spalten eingefuegt werden koennen
									   InsertIdentityOperation.CLEAN_INSERT :
									   CLEAN_INSERT;
		dbOp.execute(dbunitConn, xmlDataset);
		
		dbunitConn.close();
		jdbcConn.close();
		
		reloaded = true;
		LOGGER.info(dbReload.url + " wurde neu geladen");
	}
	
	/**
	 * Beim Start als eigenstaendiges Java-Programm mit einem zusaetzlichen Argument fuer die VM, z.B.:
	 * -javaagent:C:/Software/spring/dist/org.springframework.instrument-3.0.1.RELEASE.jar
	 * Anpassung fuer den Pfad zur Spring-Installation ist erforderlich!
	 */
	public static void main(String [] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException, DatabaseUnitException, IOException {
		reload();
	}
}
