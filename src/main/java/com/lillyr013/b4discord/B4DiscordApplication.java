package com.lillyr013.b4discord;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@RestController
public class B4DiscordApplication extends ListenerAdapter {

	public static HashMap<String, String> userLastMessage = new HashMap<>();
	public static JDA jda;

	public static void main(String[] args) {

		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, String> configValues = new HashMap<>();
		byte[] mapData = {};
		try {
			mapData = Files.readAllBytes(Paths.get("botConfig.json"));
		} catch (IOException e) {
			System.out.println("Failed to read config file");
			System.exit(2);
		}

		try {
			configValues = objectMapper.readValue(mapData, new TypeReference<HashMap<String,String>>() {});
		} catch (IOException e) {
			System.out.println("Failed to process config file");
			System.exit(3);
		}

		String token = configValues.get("token");

		EnumSet<GatewayIntent> intents = EnumSet.of(
				GatewayIntent.GUILD_MESSAGES,
				GatewayIntent.GUILD_VOICE_STATES,
				GatewayIntent.MESSAGE_CONTENT
		);

		jda = JDABuilder.createDefault(token, intents)
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

		String id = author.getId();
		userLastMessage.put(id, content);

	}

	@GetMapping("/")
	public String mainPage() {
        try (InputStream in = B4DiscordApplication.class.getResourceAsStream("/BOOT-INF/classes/static/html/index.html")) {
			if(in != null) {
				return new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
			else {
				return "index.html is empty...";
			}
        } catch (IOException e) {
            return "Error reading index.html...";
        }
    }

	@GetMapping("/portal")
	public String portal() {
		try (InputStream in = B4DiscordApplication.class.getResourceAsStream("/BOOT-INF/classes/static/html/portal.html")) {
			if(in != null) {
				return new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
			else {
				return "portal.html is still empty...";
			}
		} catch (IOException e) {
			return "Error reading portal.html...";
		}
	}

	@GetMapping("/scripts/portal.js")
	public String portalScript() {
		try (InputStream in = B4DiscordApplication.class.getResourceAsStream("/BOOT-INF/classes/static/js/portal.js")) {
			if(in != null) {
				return new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
			else {
				return "portal.js is still empty...";
			}
		} catch (IOException e) {
			return "Error reading portal.js...";
		}
	}

	@GetMapping("/lastMessage/{userID}")
	public String getLastMessage(@PathVariable String userID) {

		return userLastMessage.getOrDefault(userID, "No messages detected!");

	}

	@GetMapping("/voiceChannel/{userID}")
	public String getVoiceChannel(@PathVariable String userID) {

		User user = jda.getUserById(userID);
		if(user == null) {
			return "User not found";
		}

		List<Guild> sharedGuilds = user.getMutualGuilds();
		if(sharedGuilds.isEmpty()) {
			return "No shared server";
		}

		for(Guild g : sharedGuilds) {
			Member m = g.getMember(UserSnowflake.fromId(userID));
			if(m == null) {
				return "Error viewing member - insufficient permissions?";
			}
			GuildVoiceState memberVoiceState = m.getVoiceState();
			if(memberVoiceState == null) {
				return "Error viewing member voice state - insufficient permissions?";
			}
			else {
				if(memberVoiceState.inAudioChannel()) {
					AudioChannelUnion voiceChannel = memberVoiceState.getChannel();
					if(voiceChannel == null) {
						return "Error viewing voice channel id - insufficient permissions?";
					}
					String response = "Voice channel ID: " + voiceChannel.getId();
					response += "</br>Voice channel name is #" + voiceChannel.getName();
					return response;
				}
			}
		}
		return "No VC found";
	}

}