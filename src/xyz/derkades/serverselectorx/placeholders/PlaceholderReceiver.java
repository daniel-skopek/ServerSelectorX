package xyz.derkades.serverselectorx.placeholders;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import xyz.derkades.serverselectorx.Main;

public class PlaceholderReceiver {

    private Map<String, Server> servers = Collections.emptyMap();

    private List<String> placeholderServers;
    private String networkId;
    private final String lobbyId;

    public PlaceholderReceiver() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getPlugin(), this::updatePlaceholders, 0, 5*20);
        this.lobbyId = UUID.randomUUID().toString();

    }

    public void loadConfiguration() {
        final FileConfiguration config = Main.getConfigurationManager().getServerConfiguration();
        this.placeholderServers = config.getStringList("placeholder-servers");
        this.networkId = config.getString("network-id");
    }

    public void updatePlaceholders() {
        if (this.placeholderServers.size() == 0 || this.networkId == null) {
            return;
        }

        // Update placeholders from first server that works
        for (final String placeholderServer : this.placeholderServers) {
            try {
                this.updatePlaceholdersFrom(placeholderServer);
                break;
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void updatePlaceholdersFrom(final String placeholderServer) throws IOException {
        final JsonArray jsonPlayersArray = new JsonArray(Bukkit.getOnlinePlayers().size());
        for (final Player player : Bukkit.getOnlinePlayers()) {
            jsonPlayersArray.add(player.getUniqueId().toString());
        }

        final JsonObject requestJson = new JsonObject();
        requestJson.add("players", jsonPlayersArray);
        requestJson.addProperty("network", this.networkId);
        requestJson.addProperty("lobby", this.lobbyId);

        final URLConnection connection = URI.create(placeholderServer + "/lobby").toURL().openConnection();
        connection.setDoOutput(true);
        try (OutputStream out = connection.getOutputStream()) {
            final byte[] data = requestJson.toString().getBytes();
            out.write(data);
        }

        try (InputStream in = connection.getInputStream()) {
            final byte[] data = in.readAllBytes();
            final JsonObject repsonseJson = JsonParser.parseString(new String(data)).getAsJsonObject();
            this.servers = parseResponse(repsonseJson);
        }
    }

    public Map<String, Server> getServers() {
        return this.servers;
    }

    public @Nullable Server getServer(final String name) {
        return this.servers.get(name);
    }

    private static Map<String, Server> parseResponse(final JsonObject response) {
        final JsonObject serversObject = response.get("servers").getAsJsonObject();

        final Map<String, Server> newServers = new HashMap<>();

        for (final Map.Entry<String, JsonElement> entry : serversObject.entrySet()) {
            final String serverName = entry.getKey();
            final JsonObject serverData = entry.getValue().getAsJsonObject();

            final Map<String, Placeholder> parsedPlaceholders = new HashMap<>();

            // Global placeholders
            for (final JsonElement elem : serverData.get("global").getAsJsonArray()) {
                final JsonObject obj = elem.getAsJsonObject();
                final String key = obj.get("key").getAsString();
                final String value = obj.get("val").getAsString();
                parsedPlaceholders.put(key, new GlobalPlaceholder(key, value));
            }

            // Player placeholders
            for (final JsonElement elem : serverData.get("players").getAsJsonArray()) {
                final JsonObject obj = elem.getAsJsonObject();
                final String key = obj.get("key").getAsString();
                final JsonObject values = obj.get("val").getAsJsonObject();
                final Map<UUID, String> parsedValues = new HashMap<>();
                for (final String uuid : values.keySet()) {
                    parsedValues.put(UUID.fromString(uuid), values.get(uuid).getAsString());
                }
                parsedPlaceholders.put(key, new PlayerPlaceholder(key, parsedValues));
            }

            newServers.put(serverName, new Server(serverName, parsedPlaceholders));
        }

        return newServers;
    }

}
