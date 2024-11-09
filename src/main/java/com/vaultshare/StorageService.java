package com.vaultshare;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class StorageService {
	private final SettingsService settingsService;
	private final CryptoService cryptoService;

	public StorageService(SettingsService settingsService, CryptoService cryptoService) {
		this.settingsService = settingsService;
		this.cryptoService = cryptoService;
	}

	public void writeSingleFile(MultipartFile file, Path path, byte[] encryptKey) throws Exception {
		try (InputStream inputStream = file.getInputStream();
			 OutputStream outputStream = Files.newOutputStream(path)) {
			copy(inputStream, outputStream);
		}
		if (encryptKey != null) {
			cryptoService.encryptFile(path, encryptKey);
		}
	}

	public void writeZip(MultipartFile[] files, Path path, byte[] encryptKey) throws Exception {
		try (OutputStream outputStream = Files.newOutputStream(path);
			 ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
			for (MultipartFile file : files) {
				if (file.isEmpty()) {
					continue;
				}
				String fileName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
				zipOutputStream.putNextEntry(new ZipEntry(fileName));
				try (InputStream inputStream = file.getInputStream()) {
					copy(inputStream, zipOutputStream);
				}
				zipOutputStream.closeEntry();
			}
		}
		if (encryptKey != null) {
			cryptoService.encryptFile(path, encryptKey);
		}
	}

	public void writeText(String text, Path path, byte[] encryptKey) throws Exception {
		Files.writeString(path, text);
		if (encryptKey != null) {
			cryptoService.encryptFile(path, encryptKey);
		}
	}

	private void copy(InputStream inputStream, OutputStream outputStream) throws Exception {
		byte[] buffer = new byte[(int) (1024 * settingsService.get().getStreamSizeLimitKB())];
		for (int read; (read = inputStream.read(buffer)) != -1; ) {
			outputStream.write(buffer, 0, read);
			cryptoService.throttle();
		}
	}
}
