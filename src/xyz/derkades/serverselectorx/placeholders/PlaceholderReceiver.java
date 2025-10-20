package xyz.derkades.serverselectorx.placeholders;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import xyz.derkades.serverselectorx.Main;

public class PlaceholderReceiver implements MqttCallback {

    private static final String TOPIC_PREFIX = "ssx1";

    private final Map<String, Server> servers = new HashMap<>();
    private final MqttAsyncClient mqttClient;
    private final String id;
    private final String password;

    public PlaceholderReceiver(final String id, final String password) {
        this.id = id;
        this.password = password;
        try {
            this.mqttClient = new MqttAsyncClient("tcp://10.0.1.1", UUID.randomUUID().toString(),
                    new MemoryPersistence());
            this.mqttClient.subscribe(TOPIC_PREFIX + "/" + id + "/placeholders", 0);
            this.mqttClient.setCallback(this);
        } catch (final MqttException e) {
            throw new RuntimeException("MQTT failed to setup");
        }

        // TODO periodically send list of players to /players topic
    }

    public void connect() {
        try {
            final MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(5);
            this.mqttClient.connect(options);
        } catch (final MqttException e) {
            Main.getPlugin().getLogger().log(Level.WARNING, "MQTT failed to connect", e);
        }
    }

    public void close() {
        try {
            this.mqttClient.close();
        } catch (final MqttException e) {
            Main.getPlugin().getLogger().log(Level.WARNING, "MQTT failed to close", e);
        }
    }

    @Override
    public void connectionLost(final Throwable cause) {
        Main.getPlugin().getLogger().warning("Connection lost to MQTT server: " + cause.getMessage());
    }

    @Override
    public void messageArrived(final String topic, final MqttMessage message) throws Exception {
        final byte[] data = message.getPayload();
        final JsonObject root = JsonParser.parseString(data.toString()).getAsJsonObject();

        final String serverName = root.get("server").getAsString();
        final Server server = this.getServer(serverName);

        final Map<String, Placeholder> parsedPlaceholders = new HashMap<>();

        // Global placeholders
        for (final JsonElement elem : root.get("global").getAsJsonArray()) {
            final JsonObject obj = elem.getAsJsonObject();
            final String key = obj.get("key").getAsString();
            final String value = obj.get("val").getAsString();
            parsedPlaceholders.put(key, new GlobalPlaceholder(key, value));
        }

        // Player placeholders
        for (final JsonElement elem : root.get("players").getAsJsonArray()) {
            final JsonObject obj = elem.getAsJsonObject();
            final String key = obj.get("key").getAsString();
            final JsonObject values = root.get("val").getAsJsonObject();
            final Map<UUID, String> parsedValues = new HashMap<>();
            for (final String uuid : values.keySet()) {
                parsedValues.put(UUID.fromString(uuid), values.get(uuid).getAsString());
            }
            parsedPlaceholders.put(key, new PlayerPlaceholder(key, parsedValues));
        }

        server.updatePlaceholders(null);
    }

    @Override
    public void deliveryComplete(final IMqttDeliveryToken token) {
    }

    public Map<String, Server> getServers() {
        return this.servers;
    }

    public Server getServer(final String name) {
        Server server = this.servers.get(name);
        if (server == null) {
            server = new Server(name);
            this.servers.put(name, server);
        }
        return server;
    }

    public void clearServers() {
        this.servers.clear();
    }

}
