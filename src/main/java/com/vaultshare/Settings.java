package com.vaultshare;

public class Settings {
	private long fileSizeLimitMB = 1024;
	private long textSizeLimitMB = 10;
	private long streamSizeLimitKB = 1024;
	private long streamThrottleMS = 25;
	private int pbkdf2Iterations = 100000;
	private long cmdUploadDefaultDurationMinute = 10;
	private boolean enablePassword = false;
	private String password = "password";

	public long getFileSizeLimitMB() {
		return fileSizeLimitMB;
	}

	public void setFileSizeLimitMB(long fileSizeLimitMB) {
		this.fileSizeLimitMB = fileSizeLimitMB;
	}

	public long getTextSizeLimitMB() {
		return textSizeLimitMB;
	}

	public void setTextSizeLimitMB(long textSizeLimitMB) {
		this.textSizeLimitMB = textSizeLimitMB;
	}

	public long getStreamSizeLimitKB() {
		return streamSizeLimitKB;
	}

	public void setStreamSizeLimitKB(long streamSizeLimitKB) {
		this.streamSizeLimitKB = streamSizeLimitKB;
	}

	public long getStreamThrottleMS() {
		return streamThrottleMS;
	}

	public void setStreamThrottleMS(long streamThrottleMS) {
		this.streamThrottleMS = streamThrottleMS;
	}

	public int getPbkdf2Iterations() {
		return pbkdf2Iterations;
	}

	public void setPbkdf2Iterations(int pbkdf2Iterations) {
		this.pbkdf2Iterations = pbkdf2Iterations;
	}

	public long getCmdUploadDefaultDurationMinute() {
		return cmdUploadDefaultDurationMinute;
	}

	public void setCmdUploadDefaultDurationMinute(long cmdUploadDefaultDurationMinute) {
		this.cmdUploadDefaultDurationMinute = cmdUploadDefaultDurationMinute;
	}

	public boolean isEnablePassword() {
		return enablePassword;
	}

	public void setEnablePassword(boolean enablePassword) {
		this.enablePassword = enablePassword;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
