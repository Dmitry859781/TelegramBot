package bot.handlers.notes;

import bot.TelegramBot;
import bot.fsm.BotFSM;
import bot.handlers.StateHandler;
import bot.note.NoteService;

import java.sql.SQLException;

public class HandleAwaitingNoteNewText implements StateHandler {

    private final NoteService noteService = NoteService.getInstance();
    private final BotFSM fsm = BotFSM.getInstance();

    @Override
    public void handle(Long userId, Long chatId, String text, TelegramBot bot) {
        String oldNoteName = (String) fsm.getTempData(userId, "oldNoteName");
        String newNoteName = (String) fsm.getTempData(userId, "newNoteName");
        String currentNoteText = (String) fsm.getTempData(userId, "currentNoteText");

        String finalText;
        if ("-".equals(text.trim())) {
            finalText = currentNoteText;
        } else if (text == null || text.trim().isEmpty()) {
            bot.sendMessage(chatId, "Текст заметки не может быть пустым. Попробуйте снова или введите /cancel.");
            return;
        } else {
            finalText = text.trim();
        }
        
        // Обновляем
        try {
            noteService.updateNote(userId, oldNoteName, newNoteName, finalText);
            bot.sendMessage(chatId, "Заметка успешно обновлена!\nНовое имя: \"" + newNoteName + "\"");
        } catch (SQLException e) {
            e.printStackTrace();
            bot.sendMessage(chatId, "Ошибка при сохранении изменений в заметке. Попробуйте позже.");
        } finally {
            fsm.resetState(userId);
        }
    }
}