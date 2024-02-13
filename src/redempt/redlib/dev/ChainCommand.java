package redempt.redlib.dev;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import redempt.redlib.commandmanager.ArgType;
import redempt.redlib.commandmanager.CommandHook;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class ChainCommand {

    static {
        try {
            Field field = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            SimpleCommandMap map = (SimpleCommandMap) field.get(Bukkit.getPluginManager());
            Class<?> clazz = map.getClass();
            while (!clazz.getSimpleName().equals("SimpleCommandMap")) {
                clazz = clazz.getSuperclass();
            }
            Field mapField = clazz.getDeclaredField("knownCommands");
            mapField.setAccessible(true);
            knownCommands = (Map<String, org.bukkit.command.Command>) mapField.get(map);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Command> knownCommands;

    @CommandHook("commandchain")
    public void commandChain(CommandSender sender, String command) {
        String[] split = command.split(";");
        for (String cmd : split) {
            Bukkit.dispatchCommand(sender, cmd.trim());
        }
    }

    public ArgType<String> getArgType() {
        return new ArgType<>("commandchain", s -> s).setTab((c, s) -> {
            int i = s.length - 1;
            while (i > 0 && !s[i].endsWith(";")) {
                i--;
            }
            if (i + 1 < s.length && s[i].endsWith(";")) {
                i++;
            }
            if (s.length - i == 0 || s.length - i == 1) {
                return new ArrayList<>(knownCommands.keySet());
            }
            Command cmd = knownCommands.get(s[i]);
            if (cmd != null) {
                return cmd.tabComplete(c, s[i], Arrays.copyOfRange(s, i + 1, s.length));
            }
            return new ArrayList<>();
        });
    }

}
