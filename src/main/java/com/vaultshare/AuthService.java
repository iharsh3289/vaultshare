package com.vaultshare;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
	private final Map<String, Instant> sessions = new ConcurrentHashMap<>();
	private final SecureRandom secureRandom = new SecureRandom();
	private final SettingsService settingsService;

	public AuthService(SettingsService settingsService) {
		this.settingsService = settingsService;
	}

	public boolean authenticate(String key, HttpServletResponse response) {
		if (!settingsService.get().getPassword().equals(key)) {
			return false;
		}

		byte[] randomBytes = new byte[32];
		secureRandom.nextBytes(randomBytes);
		String session = Base64.getEncoder().encodeToString(randomBytes);
		sessions.put(session, Instant.now().plus(Duration.ofHours(24)));

		ResponseCookie cookie = ResponseCookie.from("session", session)
				.path("/")
				.httpOnly(true)
				.maxAge(Duration.ofHours(24))
				.sameSite("Lax")
				.build();
		response.addHeader("Set-Cookie", cookie.toString());
		return true;
	}

	public boolean validSession(HttpServletRequest request) {
		if (!settingsService.get().isEnablePassword()) {
			return true;
		}

		String session = sessionCookie(request);
		if (session == null) {
			return false;
		}

		Instant expires = sessions.get(session);
		if (expires == null || expires.isBefore(Instant.now())) {
			sessions.remove(session);
			return false;
		}
		return true;
	}

	public void deleteSession(HttpServletRequest request) {
		String session = sessionCookie(request);
		if (session != null) {
			sessions.remove(session);
		}
	}

	@Scheduled(fixedDelay = 60_000)
	void cleanupSessions() {
		Instant now = Instant.now();
		sessions.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
	}

	private String sessionCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if ("session".equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}
}
