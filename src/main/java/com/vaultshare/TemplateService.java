package com.vaultshare;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class TemplateService {
	public String notFound() throws IOException {
		return read("templates/notFound.html");
	}

	public String index() throws IOException {
		return read("static/index.html");
	}

	public String auth(String path) throws IOException {
		return read("templates/authTemplate.html").replace("{{path}}", HtmlUtils.htmlEscape(path));
	}

	public String paste(String text, boolean burn) throws IOException {
		String rawButton = burn ? "" : "<button onclick=\"getRawLink()\" style=\"width: 200px;\">Copy raw link</button>";
		return read("templates/pasteTemplate.html")
				.replace("{{text}}", HtmlUtils.htmlEscape(text))
				.replace("{{rawButton}}", rawButton);
	}

	private String read(String path) throws IOException {
		return StreamUtils.copyToString(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
	}
}
