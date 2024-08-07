package com.lillyr013.b4discord;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

@SpringBootApplication
@RestController
public class B4DiscordApplication extends ListenerAdapter {

	public static HashMap<String, String> userLastMessage = new HashMap<>();
	public static JDA jda;
	public static ObjectMapper mapper = new ObjectMapper();
	public static Config configValues;

	public static void main(String[] args) {

        String configJSON = "";
        try {
            configJSON = Files.readString(Paths.get("botConfig.json"));
        } catch (IOException e) {
            System.out.println("Failed to read config file");
            System.exit(1);
        }
        try {
            configValues = mapper.readValue(configJSON, Config.class);
        } catch (JsonProcessingException e) {
            System.out.println("Failed to parse config json");
			System.exit(2);
        }

        String token = configValues.token;

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

	public UserData getUserObject(String token) {
		URI path = URI.create("https://discord.com/api/users/@me");
		String userJSON = "";
		try {
			URL url = path.toURL();
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("Authorization", "Bearer " + token);
			con.connect();
			InputStream resp = con.getInputStream();
			userJSON = new String(resp.readAllBytes(), StandardCharsets.UTF_8);
			con.disconnect();
		} catch (IOException e) {
			System.out.println("Issue with connecting to Discord API");
		}
		if(userJSON.isEmpty()) {
			System.out.println("No response from Discord API...");
			return null;
		}
		else {
            try {
				System.out.println(userJSON);
                return mapper.readValue(userJSON, UserData.class);
            } catch (JsonProcessingException e) {
				System.out.println("Error parsing user object.." + e.getMessage());
                return null;
            }
        }
	}

	@PostMapping("/join/{token}")
	public void join(@PathVariable String token) {
		UserData userObj = getUserObject(token);
		if(userObj != null) {
			AudioChannelUnion vc = getAudioChannelUnion(userObj.id);
			if(vc != null) {
				AudioManager manager = vc.getGuild().getAudioManager();
				manager.openAudioConnection(vc.asVoiceChannel());
			}
		}
    }

	@GetMapping("/lastMessage/{token}")
	public String getLastMessage(@PathVariable String token) {
		UserData userObj = getUserObject(token);
		if(userObj != null) {
			return userLastMessage.getOrDefault(userObj.id, "No messages detected!");
		}
		return "Not Authorized";
	}

	public static AudioChannelUnion getAudioChannelUnion(String id) {
		User user = jda.getUserById(id);
		if(user == null) {
			return null;
		}

		List<Guild> sharedGuilds = user.getMutualGuilds();
		if(sharedGuilds.isEmpty()) {
			return null;
		}

		for(Guild g : sharedGuilds) {
			Member m = g.getMember(UserSnowflake.fromId(id));
			if (m == null) {
				continue;
			}
			GuildVoiceState memberVoiceState = m.getVoiceState();
			if (memberVoiceState != null) {
				if (memberVoiceState.inAudioChannel()) {
					return memberVoiceState.getChannel();
				}
			}
		}
		return null;
	}

	@GetMapping("/voiceChannel/{token}")
	public String getVoiceChannel(@PathVariable String token) {
		UserData userObj = getUserObject(token);
		if(userObj == null) {
			return "Not Authorized";
		}
		AudioChannelUnion voiceChannel = getAudioChannelUnion(userObj.id);
		if(voiceChannel == null) {
			return "No voice channel Found";
		}
		String response = "Voice channel ID: " + voiceChannel.getId();
		response += "</br>Voice channel name is #" + voiceChannel.getName();
		return response;

	}

}