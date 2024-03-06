package com.b4discord.main;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@RestController
public class B4DiscordApplication extends ListenerAdapter {

	public static String lastMessage = "Hi There";

	public static void main(String[] args) {

		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, String> configValues = new HashMap<>();
		byte[] mapData = {};
        try {
            mapData = Files.readAllBytes(Paths.get("botConfig.json"));
        } catch (IOException e) {
            System.exit(1);
        }

        try {
            configValues = objectMapper.readValue(mapData, new TypeReference<HashMap<String,String>>() {});
        } catch (IOException e) {
            System.exit(1);
        }

        String token = configValues.get("token");

		EnumSet<GatewayIntent> intents = EnumSet.of(
				GatewayIntent.GUILD_MESSAGES,
				GatewayIntent.GUILD_VOICE_STATES,
				GatewayIntent.MESSAGE_CONTENT
		);

		JDABuilder.createDefault(token, intents)
				.addEventListeners(new B4DiscordApplication())
				.build();
		SpringApplication.run(B4DiscordApplication.class, args);

	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		Message message = event.getMessage();
		User author = message.getAuthor();
		String content = message.getContentRaw();

		if(author.isBot()) {
			return;
		}

		if(!event.isFromGuild()) {
			return;
		}

		if(content.startsWith("!!")) {
			String arg = content.substring(2);
			event.getChannel().sendMessage(arg).complete();
			lastMessage = arg;
		}

	}

	@GetMapping("/")
	public String mainPage() {
		return "<html><body><p>" + lastMessage + "</p></body></html>";
	}

}
