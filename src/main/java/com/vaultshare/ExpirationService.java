package com.vaultshare;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@Service
public class ExpirationService {
	private final DataRepository repository;

	public ExpirationService(DataRepository repository) {
		this.repository = repository;
	}

	@Scheduled(fixedDelay = 10_000)
	public void deleteExpiredData() {
		try {
			for (StoredData data : repository.findExpired(Instant.now().getEpochSecond())) {
				repository.deleteById(data.id());
				Files.deleteIfExists(Path.of(data.filePath()));
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}
}
