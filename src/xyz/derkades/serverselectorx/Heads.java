package xyz.derkades.serverselectorx;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.gson.JsonObject;

public class Heads {

	private final Map<String, HeadHandler> handlers = new HashMap<>();

	Heads(final JavaPlugin plugin) {
		final Logger logger = plugin.getLogger();

		try {
			this.handlers.put("arc-hdb", new ArcaniaxHandler());
			logger.info("Integration with Arcaniax's Head Database plugin is active");
		} catch (final Exception e) {
			if (Main.getConfigurationManager().getMiscConfiguration().getBoolean("head-api-debug")) {
				e.printStackTrace();
			}
		}

		try {
			this.handlers.put("silent-hdb", new SilentHandler());
			logger.info("Integration with TheSilentPro's Head Database plugin is active");
		} catch (final Exception e) {
			if (Main.getConfigurationManager().getMiscConfiguration().getBoolean("head-api-debug")) {
				e.printStackTrace();
			}
		}

		this.handlers.put("uuid", new UuidHandler(plugin));
		this.handlers.put("texture", new TextureLiteralHandler());
		this.handlers.put("url", new TextureURLHandler());
	}

	public CompletableFuture<@Nullable String> getHead(final String identifier) throws InvalidConfigurationException {
		final int index = identifier.indexOf(":");

		if (index == -1) {
			throw new InvalidConfigurationException("Invalid head '" + identifier + "'. Valid syntax is 'head:<type>:<value>' or 'head:self'.");
		}

		final String type = identifier.substring(0, index);
		final String value = identifier.substring(index + 1);

		if (!this.handlers.containsKey(type)) {
			throw new InvalidConfigurationException("Invalid head type: " + type);
		}

		return this.handlers.get(type).getHeadTexture(value);
	}

	private interface HeadHandler {

		CompletableFuture<@Nullable String> getHeadTexture(String name);

	}

	private static class ArcaniaxHandler implements HeadHandler {

		private final Object apiInstance;
		private final Method getBase64Method;

		private ArcaniaxHandler() throws Exception {
			final Class<?> apiClass = Class.forName("me.arcaniax.hdb.api.HeadDatabaseAPI");
			final Constructor<?> constructor = apiClass.getConstructor();
			this.apiInstance = constructor.newInstance();
			this.getBase64Method = apiClass.getMethod("getBase64", String.class);
		}

		@Override
		public CompletableFuture<@Nullable String> getHeadTexture(final String name) {
			final CompletableFuture<String> future = new CompletableFuture<>();
			try {
				final String value = (String) this.getBase64Method.invoke(this.apiInstance, name);
				if (value == null) {
					Main.getPlugin().getLogger().warning("Head is not in head database: " + name);
				}
				future.complete(value);
			} catch (final Exception e) {
				future.completeExceptionally(e);
			}
			return future;
		}

	}

	private static class SilentHandler implements HeadHandler {

		private final Method getHeadByIdMethod;
		private final Method getValueMethod;

		private SilentHandler() throws Exception {
			final Class<?> apiClass = Class.forName("tsp.headdb.api.HeadAPI");
			this.getHeadByIdMethod = apiClass.getMethod("getHeadByID");
			final Class<?> headClass = Class.forName("tsp.headdb.implementation.Head");
			this.getValueMethod = headClass.getMethod("getValue");
		}

		@Override
		public CompletableFuture<@Nullable String> getHeadTexture(final String name) {
			final CompletableFuture<String> future = new CompletableFuture<>();
			try {
				final Object headInstance = this.getHeadByIdMethod.invoke(null);
				final String texture = (String) this.getValueMethod.invoke(headInstance);
				future.complete(texture);
			} catch (final Exception e) {
				future.completeExceptionally(e);
			}
			return future;
		}

	}

	private static class UuidHandler implements HeadHandler {

		private final JavaPlugin plugin;
		private final Map<String, String> cachedTextures = new HashMap<>();

		private UuidHandler(final JavaPlugin plugin) {
			this.plugin = plugin;
		}

		@Override
		public CompletableFuture<@Nullable String> getHeadTexture(final String name) {
			final String cachedTexture = this.cachedTextures.get(name);
			if (cachedTexture != null) {
				return CompletableFuture.completedFuture(cachedTexture);
			}

			final CompletableFuture<String> future = new CompletableFuture<>();
			Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
				final UUID uuid = UUID.fromString(name);
				Main.getPlugin().getLogger().info("Getting texture value for " + uuid + " from Mojang API");

				try {
					final HttpURLConnection connection = (HttpURLConnection) URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid).toURL().openConnection();
					try (final Reader reader = new InputStreamReader(connection.getInputStream())) {
						final JsonObject jsonResponse = Main.JSON_PARSER.parse(reader).getAsJsonObject();
						final String texture = jsonResponse.get("properties").getAsJsonArray().get(0).getAsJsonObject().get("value").getAsString();
						this.cachedTextures.put(name, texture);
						future.complete(texture);
					}
				} catch (final Exception e) {
					future.completeExceptionally(e);
				}
			});
			return future;
		}
	}

	private static class TextureLiteralHandler implements HeadHandler {

		@Override
		public CompletableFuture<@Nullable String> getHeadTexture(final String textureString) {
			return CompletableFuture.completedFuture(textureString);
		}

	}

	private static class TextureURLHandler implements HeadHandler {

		@Override
		public CompletableFuture<@Nullable String> getHeadTexture(final String textureUrl) {
			final JsonObject skinTextureJson = new JsonObject();

			final JsonObject textures = new JsonObject();
			skinTextureJson.add("textures", textures);

			final JsonObject skin = new JsonObject();
			textures.add("SKIN", skin);

			skin.addProperty("url", textureUrl);

			return CompletableFuture.completedFuture(skinTextureJson.toString());
		}

	}

}
