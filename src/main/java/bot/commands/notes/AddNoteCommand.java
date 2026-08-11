package bot.commands.notes;

import bot.TelegramBot;
import bot.commands.Command;
import bot.fsm.BotFSM;
import bot.fsm.UserState;
import bot.note.NoteService;
import org.telegram.telegrambots.meta.api.objects.Message;

public class AddNoteCommand implements Command {

    private final NoteService noteService;
    private final BotFSM fsm;

    public AddNoteCommand(NoteService noteService, BotFSM fsm) {
        this.noteService = noteService;
        this.fsm = fsm;
    }

    @Override
    public String getCommandName() {
        return "addNote";
    }

    @Override
    public String getDescription() {
        return "Добавить заметку";
    }

    @Override
    public String getUsage() {
        return "/addNote";
    }

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        Long userId = message.getFrom().getId();

        bot.sendMessage(message.getChatId(), "Введите имя добавляемой заметки.");
        fsm.setState(userId, UserState.AWAITING_NOTE_NAME_ADD);
    }
}