package com.vaultshare;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class SettingsService {
	private final Settings settings;

	public SettingsService(RuntimePaths runtimePaths, ObjectMapper objectMapper) throws IOException {
		Path settingsPath = runtimePaths.dataDir().resolve("settings.json");
		if (Files.notExists(settingsPath)) {
			try (InputStream inputStream = new ClassPathResource("data/settings.json").getInputStream()) {
				Files.copy(inputStream, settingsPath);
			}
		}

		settings = objectMapper.readValue(settingsPath.toFile(), Settings.class);
		applyEnvironmentSettings();
		applyDefaults();
	}

	public Settings get() {
		return settings;
	}

	private void applyEnvironmentSettings() {
		String enablePassword = System.getenv("VAULTSHARE_ENABLE_PASSWORD");
		if (enablePassword != null && !enablePassword.isBlank()) {
			settings.setEnablePassword(Boolean.parseBoolean(enablePassword));
		}

		String password = System.getenv("VAULTSHARE_PASSWORD");
		if (password != null && !password.isBlank()) {
			settings.setPassword(password);
		}
	}

	private void applyDefaults() {
		if (settings.getFileSizeLimitMB() <= 0) {
			settings.setFileSizeLimitMB(1024);
		}
		if (settings.getTextSizeLimitMB() <= 0) {
			settings.setTextSizeLimitMB(10);
		}
		if (settings.getStreamSizeLimitKB() <= 0) {
			settings.setStreamSizeLimitKB(1024);
		}
		if (settings.getPbkdf2Iterations() <= 0) {
			settings.setPbkdf2Iterations(100000);
		}
		if (settings.getCmdUploadDefaultDurationMinute() <= 0) {
			settings.setCmdUploadDefaultDurationMinute(10);
		}
	}
}
