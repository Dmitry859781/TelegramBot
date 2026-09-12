package bot;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import bot.commands.*;
import bot.commands.notes.*;
import bot.commands.reminders.*;
import bot.commands.timezone.SetOrEditTimezoneCommand;
import bot.fsm.BotFSM;

public class CommandRegistry {

    private final Map<String, Command> commands = new LinkedHashMap<>();
    private final BotFSM fsm = BotFSM.INSTANCE;
    
    public CommandRegistry() {
        registerBuiltInCommands();
    }

    private void registerBuiltInCommands() {
        commands.put("help", new HelpCommand(commands));
        commands.put("start", new StartCommand(commands));
        commands.put("about", new AboutCommand());
        commands.put("author", new AuthorCommand());
        commands.put("cancel", new CancelCommand(fsm));
        commands.put("addNote", new AddNoteCommand());
        commands.put("editNote", new EditNoteCommand());
        commands.put("showNote", new ShowNoteCommand());
        commands.put("removeNote", new RemoveNoteCommand());
        commands.put("addOnceReminder", new AddOnceReminderCommand());
        commands.put("addRecurringReminder", new AddRecurringReminderCommand());
        commands.put("editOnceReminder", new EditOnceReminderCommand());
        commands.put("editRecurringReminder", new EditRecurringReminderCommand());
        commands.put("removeReminder", new RemoveReminderCommand());
        commands.put("showReminder", new ShowReminderCommand());
        commands.put("setOrEditTimezone", new SetOrEditTimezoneCommand());
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