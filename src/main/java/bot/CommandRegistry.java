package bot;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import bot.commands.AboutCommand;
import bot.commands.AuthorCommand;
import bot.commands.Command;
import bot.commands.HelpCommand;
import bot.commands.StartCommand;
import bot.commands.notes.*;
import bot.fsm.BotFSM;
import bot.note.NoteService;

public class CommandRegistry {

    private final Map<String, Command> commands = new HashMap<>();
    private final NoteService noteService = NoteService.INSTANCE;
    private final BotFSM fsm = BotFSM.INSTANCE;
    
    public CommandRegistry() {
        registerBuiltInCommands();
    }

    private void registerBuiltInCommands() {
        commands.put("help", new HelpCommand(commands));
        commands.put("start", new StartCommand(commands));
        commands.put("about", new AboutCommand());
        commands.put("author", new AuthorCommand());
        commands.put("addnote", new AddNoteCommand(noteService, fsm));
        commands.put("addNote", new AddNoteCommand(noteService, fsm));
        commands.put("editNote", new EditNoteCommand(noteService, fsm));
        commands.put("showNote", new ShowNoteCommand(noteService, fsm));
        commands.put("removeNote", new RemoveNoteCommand(noteService, fsm));
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