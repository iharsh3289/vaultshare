package com.vaultshare;

import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class DataRepository {
	private final String jdbcUrl;

	public DataRepository(RuntimePaths runtimePaths) throws SQLException {
		Path databasePath = runtimePaths.dataDir().resolve("database.db");
		this.jdbcUrl = "jdbc:sqlite:" + databasePath.toAbsolutePath();
		createTable();
	}

	public synchronized void insert(StoredData data) throws SQLException {
		String sql = """
				INSERT INTO data (id, type, fileName, filePath, burn, expire, passwordHash, passwordSalt, encryptSalt)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
				""";
		try (Connection connection = connection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, data.id());
			statement.setString(2, data.type());
			statement.setString(3, data.fileName());
			statement.setString(4, data.filePath());
			statement.setString(5, data.burn() ? "1" : "0");
			statement.setString(6, Long.toString(data.expire()));
			statement.setString(7, data.passwordHash());
			statement.setString(8, data.passwordSalt());
			statement.setString(9, data.encryptSalt());
			statement.executeUpdate();
		}
	}

	public synchronized Optional<StoredData> findById(String id) throws SQLException {
		String sql = "SELECT id, type, fileName, filePath, burn, expire, passwordHash, passwordSalt, encryptSalt FROM data WHERE id = ?";
		try (Connection connection = connection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, id);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					return Optional.of(map(resultSet));
				}
			}
		}
		return Optional.empty();
	}

	public synchronized boolean existsById(String id) throws SQLException {
		try (Connection connection = connection();
			 PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM data WHERE id = ?")) {
			statement.setString(1, id);
			try (ResultSet resultSet = statement.executeQuery()) {
				return resultSet.next();
			}
		}
	}

	public synchronized List<StoredData> findExpired(long unixTime) throws SQLException {
		List<StoredData> expired = new ArrayList<>();
		String sql = "SELECT id, type, fileName, filePath, burn, expire, passwordHash, passwordSalt, encryptSalt FROM data WHERE expire <= ?";
		try (Connection connection = connection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, Long.toString(unixTime));
			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					expired.add(map(resultSet));
				}
			}
		}
		return expired;
	}

	public synchronized void deleteById(String id) throws SQLException {
		try (Connection connection = connection();
			 PreparedStatement statement = connection.prepareStatement("DELETE FROM data WHERE id = ?")) {
			statement.setString(1, id);
			statement.executeUpdate();
		}
	}

	private Connection connection() throws SQLException {
		Connection connection = DriverManager.getConnection(jdbcUrl);
		try (Statement statement = connection.createStatement()) {
			statement.execute("PRAGMA busy_timeout = 5000");
		}
		return connection;
	}

	private void createTable() throws SQLException {
		String sql = """
				CREATE TABLE IF NOT EXISTS data (
					id TEXT NOT NULL,
					type TEXT NOT NULL,
					fileName TEXT NOT NULL,
					filePath TEXT NOT NULL,
					burn TEXT NOT NULL,
					expire TEXT NOT NULL,
					passwordHash TEXT NOT NULL,
					passwordSalt TEXT NOT NULL,
					encryptSalt TEXT NOT NULL
				)
				""";
		try (Connection connection = connection();
			 Statement statement = connection.createStatement()) {
			statement.execute(sql);
		}
	}

	private StoredData map(ResultSet resultSet) throws SQLException {
		String burn = resultSet.getString("burn");
		return new StoredData(
				resultSet.getString("id"),
				resultSet.getString("type"),
				resultSet.getString("fileName"),
				resultSet.getString("filePath"),
				"1".equals(burn) || "true".equalsIgnoreCase(burn),
				Long.parseLong(resultSet.getString("expire")),
				resultSet.getString("passwordHash"),
				resultSet.getString("passwordSalt"),
				resultSet.getString("encryptSalt")
		);
	}
}
