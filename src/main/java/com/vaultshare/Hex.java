package com.vaultshare;

public final class Hex {
	private Hex() {
	}

	public static String encode(byte[] bytes) {
		StringBuilder builder = new StringBuilder(bytes.length * 2);
		for (byte value : bytes) {
			builder.append(String.format("%02x", value));
		}
		return builder.toString();
	}

	public static byte[] decode(String hex) {
		int length = hex.length();
		byte[] bytes = new byte[length / 2];
		for (int i = 0; i < length; i += 2) {
			bytes[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
		}
		return bytes;
	}
}
