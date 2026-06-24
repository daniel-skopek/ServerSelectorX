package nl.rslot.ssx.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

public class CommandMapUtil {

	public static CommandMap getCommandMap() {
		try {
			final Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
			field.setAccessible(true);
			return (CommandMap) field.get(Bukkit.getServer());
		} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public static void registerCommand(final String name, final Command command) {
		getCommandMap().register(name, command);
	}

	@SuppressWarnings({ "unchecked" })
	public static Map<String, Command> getKnownCommands() {
		try {
			final CommandMap map = getCommandMap();
			return (Map<String, Command>) map.getClass().getMethod("getKnownCommands").invoke(map);
		} catch (final InvocationTargetException | IllegalAccessException | SecurityException | NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	public static void unregisterCommand(final Command command) {
		final List<String> names = new ArrayList<>();
		names.add(command.getName());
		names.addAll(command.getAliases());
		command.unregister(getCommandMap());
		names.forEach(getKnownCommands()::remove);
	}

}
