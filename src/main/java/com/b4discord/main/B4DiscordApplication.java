package com.b4discord.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootApplication
@RestController
public class B4DiscordApplication {

	public static void main(String[] args) {
		SpringApplication.run(B4DiscordApplication.class, args);
	}

	@GetMapping("/")
	public String mainPage() {
		return "<html><body><p>B4 Discord</p></body></html>";
	}

}
