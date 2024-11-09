package com.vaultshare;

public record StoredData(
		String id,
		String type,
		String fileName,
		String filePath,
		boolean burn,
		long expire,
		String passwordHash,
		String passwordSalt,
		String encryptSalt
) {
	public boolean passwordProtected() {
		return passwordHash != null && !passwordHash.isBlank();
	}
}
