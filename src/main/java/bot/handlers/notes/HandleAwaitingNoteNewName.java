package bot.handlers.notes;

import bot.TelegramBot;
import bot.fsm.BotFSM;
import bot.fsm.UserState;
import bot.handlers.StateHandler;
import bot.note.NoteService;

import java.sql.SQLException;

public class HandleAwaitingNoteNewName implements StateHandler {

    private final BotFSM fsm = BotFSM.getInstance();
    private final NoteService noteService = NoteService.getInstance();

    @Override
    public void handle(Long userId, Long chatId, String text, TelegramBot bot) {
        if (text == null || text.trim().isEmpty()) {
            bot.sendMessage(chatId, "Имя заметки не может быть пустым. Попробуйте снова или введите /cancel.");
            return;
        }

        String cleanName = text.trim();

        try {
            // Получаем текущий текст заметки. Если null - заметки с таким именем не существует.
            String currentText = noteService.getNote(userId, cleanName);
            
            if (currentText == null) {
                bot.sendMessage(chatId, "Заметка с именем \"" + cleanName + "\" не найдена. Проверьте имя или введите /cancel.");
                return;
            }

            // Сохраняем текущие данные во временное хранилище FSM для последующего обновления
            fsm.setTempData(userId, "oldNoteName", cleanName);
            fsm.setTempData(userId, "currentNoteText", currentText);

            // Переходим к состоянию ожидания нового имени
            fsm.setState(userId, UserState.AWAITING_NOTE_NEW_NAME);
            bot.sendMessage(chatId, "Текущее имя: \"" + cleanName + "\"\n\nВведите новое имя заметки (или отправьте `-`, чтобы оставить без изменений):");

        } catch (SQLException e) {
            e.printStackTrace();
            bot.sendMessage(chatId, "Ошибка при поиске заметки. Попробуйте позже.");
            fsm.resetState(userId);
        }
    }
}