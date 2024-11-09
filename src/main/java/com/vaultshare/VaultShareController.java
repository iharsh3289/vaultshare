package com.vaultshare;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.CipherInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

@RestController
public class VaultShareController {
	private static final long MAX_SECONDS = 6_311_520_000L;

	private final SettingsService settingsService;
	private final AuthService authService;
	private final CryptoService cryptoService;
	private final StorageService storageService;
	private final DataRepository repository;
	private final RandomIdService randomIdService;
	private final TemplateService templateService;

	public VaultShareController(
			SettingsService settingsService,
			AuthService authService,
			CryptoService cryptoService,
			StorageService storageService,
			DataRepository repository,
			RandomIdService randomIdService,
			TemplateService templateService
	) {
		this.settingsService = settingsService;
		this.authService = authService;
		this.cryptoService = cryptoService;
		this.storageService = storageService;
		this.repository = repository;
		this.randomIdService = randomIdService;
		this.templateService = templateService;
	}

	@GetMapping("/healthz")
	public String health() {
		return "ok";
	}

	@GetMapping({"/", "/index.html"})
	public ResponseEntity<?> index(HttpServletRequest request) throws Exception {
		if (!authService.validSession(request)) {
			return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/auth.html")).build();
		}
		return html(templateService.index());
	}

	@PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<String> uploadFile(
			HttpServletRequest request,
			@RequestParam("file") MultipartFile[] files,
			@RequestParam(value = "duration", required = false) Long duration,
			@RequestParam(value = "pass", required = false, defaultValue = "") String password,
			@RequestParam(value = "burn", required = false, defaultValue = "false") boolean burn,
			@RequestParam(value = "auth", required = false, defaultValue = "") String auth
	) throws Exception {
		if (!authService.validSession(request) && !settingsService.get().getPassword().equals(auth)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		MultipartFile[] nonEmptyFiles = Arrays.stream(files).filter(file -> !file.isEmpty()).toArray(MultipartFile[]::new);
		if (nonEmptyFiles.length == 0) {
			return ResponseEntity.badRequest().build();
		}
		for (MultipartFile file : nonEmptyFiles) {
			if (file.getSize() > settingsService.get().getFileSizeLimitMB() * 1024 * 1024) {
				return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("SizeExceeded");
			}
		}

		long seconds = seconds(duration == null ? settingsService.get().getCmdUploadDefaultDurationMinute() : duration);
		if (seconds <= 0) {
			return ResponseEntity.badRequest().build();
		}

		PasswordMaterial material = passwordMaterial(password);
		Path path;
		String fileName;
		if (nonEmptyFiles.length == 1) {
			path = randomIdService.uploadPath("");
			fileName = Optional.ofNullable(nonEmptyFiles[0].getOriginalFilename()).orElse("file");
			storageService.writeSingleFile(nonEmptyFiles[0], path, material.encryptKey());
		} else {
			path = randomIdService.uploadPath(".zip");
			fileName = "files.zip";
			storageService.writeZip(nonEmptyFiles, path, material.encryptKey());
		}

		String id = randomIdService.dataId();
		repository.insert(new StoredData(
				id,
				"file",
				fileName,
				path.toString(),
				burn,
				Instant.now().getEpochSecond() + seconds,
				material.passwordHash(),
				material.passwordSalt(),
				material.encryptSalt()
		));
		return ResponseEntity.ok(host(request) + "/" + id);
	}

	@PostMapping(value = "/postText", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> uploadText(HttpServletRequest request, @RequestBody TextUploadRequest body) throws Exception {
		if (!authService.validSession(request)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		long seconds = seconds(body.duration());
		if (seconds <= 0 || body.text() == null) {
			return ResponseEntity.badRequest().build();
		}
		if (body.text().getBytes(StandardCharsets.UTF_8).length > settingsService.get().getTextSizeLimitMB() * 1024 * 1024) {
			return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("SizeExceeded");
		}

		PasswordMaterial material = passwordMaterial(Optional.ofNullable(body.pass()).orElse(""));
		Path path = randomIdService.uploadPath("");
		storageService.writeText(body.text(), path, material.encryptKey());

		String id = randomIdService.dataId();
		repository.insert(new StoredData(
				id,
				"text",
				"",
				path.toString(),
				body.burn(),
				Instant.now().getEpochSecond() + seconds,
				material.passwordHash(),
				material.passwordSalt(),
				material.encryptSalt()
		));
		return ResponseEntity.ok(host(request) + "/" + id);
	}

	@PostMapping("/auth")
	public ResponseEntity<String> auth(@RequestBody AuthRequest body, HttpServletResponse response) {
		return ResponseEntity.ok(authService.authenticate(body.key(), response) ? "done" : "wrong");
	}

	@PostMapping("/deleteSession")
	public ResponseEntity<Void> deleteSession(HttpServletRequest request) {
		authService.deleteSession(request);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/{id:[A-Za-z0-9]+}")
	public ResponseEntity<?> download(
			@PathVariable String id,
			@RequestParam(value = "key", required = false, defaultValue = "") String key,
			@RequestParam(value = "raw", required = false, defaultValue = "") String raw,
			HttpServletRequest request
	) throws Exception {
		Optional<StoredData> maybeData = repository.findById(id);
		if (maybeData.isEmpty()) {
			return html(templateService.notFound());
		}

		StoredData data = maybeData.get();
		byte[] decryptKey = null;
		if (data.passwordProtected()) {
			if (key.isBlank()) {
				return html(templateService.auth(id));
			}
			if (!passwordMatches(key, data)) {
				String referer = request.getHeader("Referer");
				if (referer == null || referer.isBlank()) {
					return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
				}
				return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(referer)).build();
			}
			decryptKey = cryptoService.passwordHash(key, Hex.decode(data.encryptSalt()));
		}

		if ("file".equals(data.type())) {
			return fileResponse(data, decryptKey);
		}

		return textResponse(data, decryptKey, "1".equals(raw));
	}

	private ResponseEntity<InputStreamResource> fileResponse(StoredData data, byte[] decryptKey) throws Exception {
		Path path = Path.of(data.filePath());
		long size = Files.size(path);
		boolean encrypted = decryptKey != null;
		long contentLength = encrypted ? size - CryptoService.NONCE_SIZE : size;

		InputStream inputStream = Files.newInputStream(path);
		if (encrypted) {
			byte[] nonce = inputStream.readNBytes(CryptoService.NONCE_SIZE);
			inputStream = new CipherInputStream(inputStream, cryptoService.decryptCipher(decryptKey, nonce));
		}

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(data.fileName()).build().toString())
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.contentLength(contentLength)
				.body(new InputStreamResource(new BurnAfterCloseInputStream(inputStream, data)));
	}

	private ResponseEntity<?> textResponse(StoredData data, byte[] decryptKey, boolean raw) throws Exception {
		Path path = Path.of(data.filePath());
		byte[] bytes = Files.readAllBytes(path);
		String text;
		if (decryptKey == null) {
			text = new String(bytes, StandardCharsets.UTF_8);
		} else {
			byte[] nonce = Arrays.copyOfRange(bytes, 0, CryptoService.NONCE_SIZE);
			byte[] encrypted = Arrays.copyOfRange(bytes, CryptoService.NONCE_SIZE, bytes.length);
			text = new String(cryptoService.decryptCipher(decryptKey, nonce).doFinal(encrypted), StandardCharsets.UTF_8);
		}

		try {
			URI uri = URI.create(text);
			if (uri.isAbsolute() && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
				burnIfNeeded(data);
				return ResponseEntity.status(HttpStatus.SEE_OTHER).location(uri).build();
			}
		} catch (IllegalArgumentException ignored) {
		}

		ResponseEntity<String> response = raw
				? ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(text)
				: html(templateService.paste(text, data.burn()));
		burnIfNeeded(data);
		return response;
	}

	private boolean passwordMatches(String key, StoredData data) {
		byte[] expected = Hex.decode(data.passwordHash());
		byte[] actual = cryptoService.passwordHash(key, Hex.decode(data.passwordSalt()));
		return Arrays.equals(expected, actual);
	}

	private PasswordMaterial passwordMaterial(String password) {
		if (password == null || password.isBlank()) {
			return new PasswordMaterial("", "", "", null);
		}
		byte[] passwordSalt = cryptoService.salt();
		byte[] encryptSalt = cryptoService.salt();
		return new PasswordMaterial(
				Hex.encode(cryptoService.passwordHash(password, passwordSalt)),
				Hex.encode(passwordSalt),
				Hex.encode(encryptSalt),
				cryptoService.passwordHash(password, encryptSalt)
		);
	}

	private long seconds(long minutes) {
		long seconds = minutes * 60;
		if (seconds > MAX_SECONDS) {
			return MAX_SECONDS;
		}
		return seconds;
	}

	private void burnIfNeeded(StoredData data) {
		if (!data.burn()) {
			return;
		}
		try {
			repository.deleteById(data.id());
			Files.deleteIfExists(Path.of(data.filePath()));
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	private String host(HttpServletRequest request) {
		String host = request.getHeader("Host");
		return host == null || host.isBlank() ? request.getServerName() : host;
	}

	private ResponseEntity<String> html(String body) {
		return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(body);
	}

	private record PasswordMaterial(String passwordHash, String passwordSalt, String encryptSalt, byte[] encryptKey) {
	}

	private class BurnAfterCloseInputStream extends FilterInputStream {
		private final StoredData data;

		protected BurnAfterCloseInputStream(InputStream inputStream, StoredData data) {
			super(inputStream);
			this.data = data;
		}

		@Override
		public void close() throws IOException {
			try {
				super.close();
			} finally {
				burnIfNeeded(data);
			}
		}
	}
}
