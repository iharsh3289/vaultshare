package com.vaultshare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@EnableScheduling
@SpringBootApplication
public class VaultShareApplication {

	public static void main(String[] args) {
		SpringApplication.run(VaultShareApplication.class, args);
	}

	@Bean
	RuntimePaths runtimePaths() throws IOException {
		Path dataDir = Path.of(System.getenv().getOrDefault("VAULTSHARE_DATA_DIR", "data"));
		Path uploadsDir = Path.of(System.getenv().getOrDefault("VAULTSHARE_UPLOAD_DIR", "uploads"));
		Files.createDirectories(dataDir);
		Files.createDirectories(uploadsDir);
		return new RuntimePaths(dataDir, uploadsDir);
	}
}
