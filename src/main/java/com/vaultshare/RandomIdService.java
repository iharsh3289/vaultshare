package com.vaultshare;

import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Instant;

@Service
public class RandomIdService {
	private static final String CHARSET = "abcdefghkmnpqrstwxyzABCDEFGHJKLMNPQRSTWXYZ23456789";

	private final SecureRandom random = new SecureRandom();
	private final RuntimePaths runtimePaths;
	private final DataRepository repository;

	public RandomIdService(RuntimePaths runtimePaths, DataRepository repository) {
		this.runtimePaths = runtimePaths;
		this.repository = repository;
	}

	public Path uploadPath(String extension) {
		while (true) {
			String fileName = Instant.now().toEpochMilli() + randomString(5) + extension;
			Path path = runtimePaths.uploadsDir().resolve(fileName);
			if (Files.notExists(path)) {
				return path;
			}
		}
	}

	public String dataId() throws SQLException {
		while (true) {
			String id = randomString(6);
			if (!repository.existsById(id)) {
				return id;
			}
		}
	}

	private String randomString(int length) {
		StringBuilder builder = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			builder.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
		}
		return builder.toString();
	}
}
