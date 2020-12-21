package com.verge.taglines;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class VergeTaglinesApplication {

	@Value("${TARGET:World}")
	String target;

	@RestController
	static class MainController {
		@GetMapping("/")
		String run() {
			try {
				return new TaglineManager().run();
			} catch (Exception e) {
				return e.toString();
			}
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(VergeTaglinesApplication.class, args);
	}

}
