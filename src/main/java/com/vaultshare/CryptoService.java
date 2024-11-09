package com.vaultshare;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

@Service
public class CryptoService {
	public static final int NONCE_SIZE = 16;

	private final SecureRandom secureRandom = new SecureRandom();
	private final SettingsService settingsService;

	public CryptoService(SettingsService settingsService) {
		this.settingsService = settingsService;
	}

	public byte[] salt() {
		byte[] salt = new byte[16];
		secureRandom.nextBytes(salt);
		return salt;
	}

	public byte[] passwordHash(String password, byte[] salt) {
		try {
			KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, settingsService.get().getPbkdf2Iterations(), 256);
			return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to generate password hash", exception);
		}
	}

	public void encryptFile(Path sourcePath, byte[] aesKey) throws Exception {
		Path temporaryPath = sourcePath.resolveSibling(sourcePath.getFileName() + ".tmp");
		byte[] nonce = new byte[NONCE_SIZE];
		secureRandom.nextBytes(nonce);

		Cipher cipher = cipher(Cipher.ENCRYPT_MODE, aesKey, nonce);
		byte[] buffer = new byte[(int) (1024 * settingsService.get().getStreamSizeLimitKB())];

		try (InputStream inputStream = Files.newInputStream(sourcePath);
			 OutputStream outputStream = Files.newOutputStream(temporaryPath)) {
			outputStream.write(nonce);
			for (int read; (read = inputStream.read(buffer)) != -1; ) {
				outputStream.write(cipher.update(buffer, 0, read));
				throttle();
			}
			outputStream.write(cipher.doFinal());
		}

		Files.delete(sourcePath);
		Files.move(temporaryPath, sourcePath);
	}

	public Cipher decryptCipher(byte[] aesKey, byte[] nonce) throws Exception {
		return cipher(Cipher.DECRYPT_MODE, aesKey, nonce);
	}

	private Cipher cipher(int mode, byte[] aesKey, byte[] nonce) throws Exception {
		Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
		cipher.init(mode, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(nonce));
		return cipher;
	}

	public void throttle() throws InterruptedException {
		long throttle = settingsService.get().getStreamThrottleMS();
		if (throttle > 0) {
			Thread.sleep(throttle);
		}
	}
}
