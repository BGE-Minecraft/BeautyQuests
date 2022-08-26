package fr.skytasul.quests.utils;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.bukkit.configuration.ConfigurationSection;
import org.mariadb.jdbc.MariaDbPoolDataSource;

import com.mysql.cj.jdbc.MysqlDataSource;

import fr.skytasul.quests.BeautyQuests;

public class Database implements Closeable {

	private ConfigurationSection config;
	private String databaseName;

	private DataSource source;

	public Database(ConfigurationSection config) throws SQLException {
		this.config = config;
		this.databaseName = config.getString("database");

		try {
			Class.forName("org.mariadb.jdbc.MariaDbPoolDataSource");
			MariaDbPoolDataSource msource = new MariaDbPoolDataSource();
			msource.setServerName(config.getString("host"));
			msource.setPortNumber(config.getInt("port"));
			msource.setDatabaseName(databaseName);
			msource.setUser(config.getString("username"));
			msource.setPassword(config.getString("password"));
			
			msource.setPoolName("beautyquests");
			msource.setMaxIdleTime(60);
			msource.setLoginTimeout(20);
			
			source = msource;
		}catch (ClassNotFoundException e) {
			MysqlDataSource msource = new MysqlDataSource();
			msource.setServerName(config.getString("host"));
			msource.setPortNumber(config.getInt("port"));
			msource.setDatabaseName(databaseName);
			msource.setUser(config.getString("username"));
			msource.setPassword(config.getString("password"));
			
			msource.setConnectTimeout(20);
			boolean ssl = config.getBoolean("ssl");
			msource.setVerifyServerCertificate(ssl);
			msource.setUseSSL(ssl);
			
			source = msource;
		}
		DebugUtils.logMessage("Created SQL data source: " + source.getClass().getName());
		// Yes, I know there is literally the same code twice.
		// Unfortunately, there is no common interface
		// between MariaDB and MySQL pool data source
		// which provides the configuration methods.
	}
	
	public void testConnection() throws SQLException {
		DebugUtils.logMessage("Trying to connect to " + config.getString("host"));
		try (Connection connection = source.getConnection()) {
			if (!connection.isValid(0))
				throw new SQLException("Could not establish database connection.");
		}
	}

	public String getDatabase() {
		return databaseName;
	}
	
	public ConfigurationSection getConfig() {
		return config;
	}

	@Override
	public void close() {
		if (source instanceof Closeable) {
			try {
				((Closeable) source).close();
			}catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public Connection getConnection() throws SQLException {
		return source.getConnection();
	}
	
	public static Database getInstance(){
		return BeautyQuests.getInstance().getBQDatabase();
	}


}
