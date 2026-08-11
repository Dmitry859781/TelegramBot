package bot.commands.notes;

import bot.TelegramBot;
import bot.commands.Command;
import bot.fsm.BotFSM;
import bot.fsm.UserState;
import bot.note.NoteService;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;

public class EditNoteCommand implements Command {

    private final NoteService noteService;
    private final BotFSM fsm;

    public EditNoteCommand(NoteService noteService, BotFSM fsm) {
        this.noteService = noteService;
        this.fsm = fsm;
    }

    @Override
    public String getCommandName() {
        return "editNote";
    }

    @Override
    public String getDescription() {
        return "Редактировать заметку";
    }

    @Override
    public String getUsage() {
        return "/editNote";
    }

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();

        try {
            List<String> noteNames = noteService.getUserNotes(userId);

            if (noteNames.isEmpty()) {
                bot.sendMessage(chatId, "У вас пока нет заметок. Добавьте первую с помощью /addNote");
                return;
            }

            StringBuilder response = new StringBuilder("Какую заметку хотите отредактировать?\n");
            for (int i = 0; i < noteNames.size(); i++) {
                response.append(i + 1).append(". ").append(noteNames.get(i)).append("\n");
            }
            bot.sendMessage(chatId, response.toString().trim());

            bot.sendMessage(chatId, "Введите имя редактируемой заметки.");

            fsm.setState(userId, UserState.AWAITING_NOTE_NAME_EDIT);
        } catch (Exception e) {
            bot.sendMessage(chatId, "Не удалось вывести список заметок. Попробуйте позже.");
        }
    }
}