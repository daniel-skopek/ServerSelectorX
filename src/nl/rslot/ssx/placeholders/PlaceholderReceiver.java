package nl.rslot.ssx.placeholders;

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
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import nl.rslot.ssx.Main;

public class PlaceholderReceiver {

    private Map<String, Server> servers = Collections.emptyMap();
    private String status;
    private long lastSuccess = 0;
    private List<String> placeholderServers = Collections.emptyList();
    private String networkId;
    private final String lobbyId;

    public PlaceholderReceiver() {
        Bukkit.getAsyncScheduler().runAtFixedRate(Main.getPlugin(), scheduledTask -> this.updatePlaceholders(), 0, 5, TimeUnit.SECONDS);
        this.lobbyId = UUID.randomUUID().toString();

    }

    public String getStatus() {
        return this.status;
    }

    public void loadConfiguration() {
        final FileConfiguration config = Main.getConfigurationManager().getServerConfiguration();
        this.placeholderServers = config.getStringList("placeholder-servers");
        this.networkId = config.getString("network-id");
    }

    public void updatePlaceholders() {
        if (this.placeholderServers.size() == 0 || this.networkId == null) {
            this.status = "Please configure network-id and placeholder-servers in plugins/ServerSelectorX/config/server.yml";
            return;
        }

        // Update placeholders from first server that works
        String newStatus = "";
        for (final String placeholderServer : this.placeholderServers) {
            try {
                this.updatePlaceholdersFrom(placeholderServer);
                this.lastSuccess = System.currentTimeMillis();
                newStatus += ChatColor.GRAY + placeholderServer + ChatColor.GREEN + " WORKING" + ChatColor.RESET + "\n";
                break;
            } catch (final Exception e) {
                newStatus += ChatColor.GRAY + placeholderServer + ChatColor.RED + " " + e.getClass().getSimpleName() + ": " + e.getMessage() + ChatColor.RESET + "\n";
            }
        }
        this.status = newStatus;

        // Clear server list if we have not been able to connect for a long time
        // Then it is better to show servers as offline instead of with incorrect info
        if (System.currentTimeMillis() - this.lastSuccess > 300_000) {
            this.servers = Collections.emptyMap();
        }
    }

    public void updatePlaceholdersFrom(final String placeholderServer) throws IOException {
        final JsonArray jsonPlayersArray = new JsonArray();
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
            final JsonObject serverObject = entry.getValue().getAsJsonObject();
            final JsonObject placeholdersObject = serverObject.get("placeholders").getAsJsonObject();

            final Map<String, Placeholder> parsedPlaceholders = new HashMap<>();

            for (final Map.Entry<String, JsonElement> placeholderEntry : placeholdersObject.entrySet()) {
                final String key = placeholderEntry.getKey();

                if (placeholderEntry.getValue().isJsonObject()) {
                    // player-specific placeholder
                    final JsonObject values = placeholderEntry.getValue().getAsJsonObject();
                    final Map<UUID, String> parsedValues = new HashMap<>();
                    for (final Map.Entry<String, JsonElement> valueEntry : values.entrySet()) {
                        parsedValues.put(UUID.fromString(valueEntry.getKey()), valueEntry.getValue().getAsString());
                    }
                    parsedPlaceholders.put(key, new PlayerPlaceholder(key, parsedValues));
                } else if (placeholderEntry.getValue().isJsonPrimitive()) {
                    // global placeholder
                    final String value = placeholderEntry.getValue().getAsString();
                    parsedPlaceholders.put(key, new GlobalPlaceholder(key, value));
                }
            }

            newServers.put(serverName, new Server(serverName, parsedPlaceholders));
        }

        return newServers;
    }

}
