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
		return "<html><body><p>Click <a href=\"https://discord.com/oauth2/authorize?client_id=1214703405872840815&response_type=token&redirect_uri=http%3A%2F%2Fb4discord.com%2Fportal&scope=identify\">here</a> to login</p></body></html>";
	}

	@GetMapping("/portal")
	public String portal() {
		return "<html><body><script>var token = window.location.href.split(\"#\")[1].split(\"access_token=\")[1].split(\"&\")[0]; var xmlHttpRequest = new XMLHttpRequest(); xmlHttpRequest.open(\"GET\", \"https://discord.com/api/users/@me\", true); xmlHttpRequest.onreadystatechange = function(){if(this.readyState == 4 && this.status == 200) {var userData = JSON.parse(this.responseText); var userID = userData.id; var xh2 = new XMLHttpRequest(); xh2.open(\"GET\", \"/lastMessage/\" + userID, true); xh2.onreadystatechange = function(){if(this.readyState == 4 && this.status == 200) { document.getElementById('lastMessage').innerHTML = xh2.responseText; }}; xh2.send(); }};xmlHttpRequest.setRequestHeader(\"Authorization\", \"Bearer \" + token); xmlHttpRequest.send();</script><p>You are logged in!</p><p id='lastMessage'></p></body></html>";
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
					return voiceChannel.getId();
				}
			}
		}
		return "No VC found";
	}

}