package bot.handlers.notes;

import java.sql.SQLException;
import java.util.List;
import bot.TelegramBot;
import bot.fsm.BotFSM;
import bot.fsm.UserState;
import bot.handlers.StateHandler;
import bot.note.NoteService;

public class HandleAwaitingNoteNameToAdd implements StateHandler{
	
	private final NoteService noteService = NoteService.getInstance();
	private final BotFSM fsm = BotFSM.getInstance();
	
	public void handle(Long userId, Long chatId, String text, TelegramBot bot) {
        if (text == null || text.trim().isEmpty()) {
            bot.sendMessage(chatId, "Имя заметки не может быть пустым. Попробуйте снова или введите /cancel");
            return;
        }

        String cleanName = text.trim();
        
        // Проверяем, существует ли заметка с таким именем
        try {
            List<String> userNotes = noteService.getUserNotes(userId);
            if (userNotes.contains(cleanName)) {
                bot.sendMessage(chatId, "Заметка с именем \"" + cleanName + "\" уже существует. Введите другое имя или /cancel.");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            bot.sendMessage(chatId, "Ошибка при проверке существования заметки. Попробуйте позже.");
            fsm.resetState(userId);
            return;
        }
        
        bot.sendMessage(chatId, "Введите текст для заметки \"" + cleanName + "\".");
        fsm.setState(userId, UserState.AWAITING_NOTE_TEXT);
        fsm.setTempData(userId, "noteName", cleanName);
    }
}
