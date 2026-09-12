package bot.handlers.notes;

import java.sql.SQLException;

import bot.TelegramBot;
import bot.fsm.BotFSM;
import bot.handlers.StateHandler;
import bot.note.NoteService;

public class HandleAwaitingNoteNameToShow implements StateHandler{
	private final NoteService noteService = NoteService.getInstance();
	private final BotFSM fsm = BotFSM.getInstance();
	
	public void handle(Long userId, Long chatId, String text, TelegramBot bot) {
        if (text == null || text.trim().isEmpty()) {
            bot.sendMessage(chatId, "Имя заметки не может быть пустым. Попробуйте снова или введите /cancel");
            return;
        }

        String cleanName = text.trim();
        try {
            String noteText = noteService.getNote(userId, cleanName);
            if (noteText != null) {
                bot.sendMessage(chatId, cleanName + "\n" + noteText);
            } else {
                bot.sendMessage(chatId, "Заметка \"" + cleanName + "\" не найдена.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            bot.sendMessage(chatId, "Не удалось получить заметку. Попробуйте позже.");
        } finally {
        fsm.resetState(userId);
        }
    }
}
