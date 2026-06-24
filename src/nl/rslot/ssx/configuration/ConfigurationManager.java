package nl.rslot.ssx.configuration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import nl.rslot.ssx.Main;

/**
 * Manages configuration files
 */
public class ConfigurationManager {

	private static final File CONFIG_DIR = new File(Main.getPlugin().getDataFolder(), "config");

	/**
	 * Copies any file in a jar file to an outside locations. Fails silently if the file already exists.
	 * @param clazz Class this method is called from
	 * @param pathToFileInJar
	 * @param outputFile
	 */
	public static void copyOutOfJar(final Class<?> clazz,
									final String pathToFileInJar,
									final File outputFile) throws IOException {
		if (!outputFile.exists()){
			final URL inputUrl = clazz.getResource(pathToFileInJar);
			try (InputStream in = inputUrl.openStream()) {
				Files.copy(in, outputFile.toPath());
			}
		}
	}

	private enum StandardConfigFile {

		SERVER(new File(CONFIG_DIR, "server.yml")),
		INVENTORY(new File(CONFIG_DIR, "inventory.yml")),
		JOIN(new File(CONFIG_DIR, "join.yml")),
		MISC(new File(CONFIG_DIR, "misc.yml"));

		private final File file;

		StandardConfigFile(final File file){
			this.file = file;
		}

		private YamlConfiguration copyLoad() throws IOException {
			copyOutOfJar(this.getClass(), "/config/" + this.file.getName(), this.file);
			return YamlConfiguration.loadConfiguration(this.file);
		}
	}

	private enum MultiConfigFile {

		COMMAND(new File(Main.getPlugin().getDataFolder(), "command"), "/command.yml", "servers.yml"),
		ITEM(new File(Main.getPlugin().getDataFolder(), "item"), "/item.yml", "compass.yml"),
		MENU(new File(Main.getPlugin().getDataFolder(), "menu"), "/menu.yml", "serverselector.yml");

		private final File dir;
		private final String defaultFileInJar;
		private final String defaultFileOutsideJar;

		MultiConfigFile(final File dir, final String defaultFileInJar,
						final String defaultFileOutsideJar) {
			this.dir = dir;
			this.defaultFileInJar = defaultFileInJar;
			this.defaultFileOutsideJar = defaultFileOutsideJar;
		}

		private void loadDirectory(final String prefix, final File dir, final Map<String, FileConfiguration> dest) {
			final File[] files = dir.listFiles();
			if (files == null) {
				return;
			}

			for (final File file : files) {
				if (file.isDirectory()) {
					this.loadDirectory(prefix + file.getName() + "/", file, dest);
				} else if (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml")) {
					dest.put(prefix + configName(file), YamlConfiguration.loadConfiguration(file));
				}
			}
		}

		private void copyLoad(final Map<String, FileConfiguration> dest) throws IOException {
			if (!this.dir.exists()) {
				this.dir.mkdir();
				copyOutOfJar(this.getClass(), this.defaultFileInJar, new File(this.dir, this.defaultFileOutsideJar));
			}

			this.loadDirectory("", this.dir, dest);
		}

		private static String configName(final File file) {
			if (file.getName().endsWith(".yml")) {
				return file.getName().substring(0, file.getName().length() - 4);
			} else if (file.getName().endsWith(".yaml")) {
				return file.getName().substring(0, file.getName().length() - 5);
			} else {
				throw new IllegalArgumentException(file.getName());
			}
		}

	}

	private final Map<StandardConfigFile, FileConfiguration> STANDARD_FILES = new EnumMap<>(StandardConfigFile.class);
	private final Map<MultiConfigFile, Map<String, FileConfiguration>> MULTI_FILES = new EnumMap<>(MultiConfigFile.class);

	public void reload() throws IOException {
		final File dir = Main.getPlugin().getDataFolder();
		final File configDir = new File(dir, "config");
		configDir.mkdirs();

		// First copy all files to the config directory

		synchronized(this.STANDARD_FILES) {
			for (final StandardConfigFile file : StandardConfigFile.values()) {
				this.STANDARD_FILES.put(file, file.copyLoad());
			}
		}

		synchronized(this.MULTI_FILES) {
			for (final MultiConfigFile file : MultiConfigFile.values()) {
				final Map<String, FileConfiguration> files = new HashMap<>();
				file.copyLoad(files);
				this.MULTI_FILES.put(file, files);
			}
		}
	}

	public @Nullable FileConfiguration getCommandConfiguration(final String name) {
		synchronized(this.MULTI_FILES) {
			return this.MULTI_FILES.get(MultiConfigFile.COMMAND).get(name);
		}
	}

	public Set<String> listCommandConfigurations() {
		synchronized(this.MULTI_FILES) {
			return this.MULTI_FILES.get(MultiConfigFile.COMMAND).keySet();
		}
	}

	public @Nullable FileConfiguration getItemConfiguration(final String name) {
		synchronized(this.MULTI_FILES) {
			return this.MULTI_FILES.get(MultiConfigFile.ITEM).get(name);
		}
	}

	public Set<String> listItemConfigurations() {
		synchronized(this.MULTI_FILES) {
			return this.MULTI_FILES.get(MultiConfigFile.ITEM).keySet();
		}
	}

	public @Nullable FileConfiguration getMenuConfiguration(final String name) {
		synchronized(this.MULTI_FILES) {
			return this.MULTI_FILES.get(MultiConfigFile.MENU).get(name);
		}
	}

	public Set<String> listMenuConfigurations() {
		synchronized(this.MULTI_FILES) {
			return this.MULTI_FILES.get(MultiConfigFile.MENU).keySet();
		}
	}

	public FileConfiguration getServerConfiguration() {
		synchronized(this.STANDARD_FILES) {
			return this.STANDARD_FILES.get(StandardConfigFile.SERVER);
		}
	}

	public FileConfiguration getInventoryConfiguration() {
		synchronized(this.STANDARD_FILES) {
			return this.STANDARD_FILES.get(StandardConfigFile.INVENTORY);
		}
	}

	public FileConfiguration getJoinConfiguration() {
		synchronized(this.STANDARD_FILES) {
			return this.STANDARD_FILES.get(StandardConfigFile.JOIN);
		}
	}

	public FileConfiguration getMiscConfiguration() {
		synchronized(this.STANDARD_FILES) {
			return this.STANDARD_FILES.get(StandardConfigFile.MISC);
		}
	}

}
