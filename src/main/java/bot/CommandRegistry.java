package bot;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import bot.commands.AboutCommand;
import bot.commands.AuthorCommand;
import bot.commands.Command;
import bot.commands.HelpCommand;

public class CommandRegistry {

    private final Map<String, Command> commands = new HashMap<>();

    public CommandRegistry() {
        registerBuiltInCommands();
    }

    private void registerBuiltInCommands() {
        commands.put("help", new HelpCommand(commands));
        commands.put("about", new AboutCommand());
        commands.put("author", new AuthorCommand());
    }

    public void registerCommand(String name, Command command) {
        commands.put(name, command);
    }

    public Command getCommand(String name) {
        return commands.get(name);
    }

    public Collection<Command> getAllCommands() {
        return commands.values();
    }
}