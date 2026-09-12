package bot.handlers.reminders;

import bot.TelegramBot;
import bot.fsm.BotFSM;
import bot.handlers.StateHandler;
import bot.reminder.ReminderService;

import java.sql.SQLException;

public class HandleAwaitingReminderNameToRemove implements StateHandler {

    private final BotFSM fsm = BotFSM.getInstance();
    private final ReminderService reminderService = ReminderService.getInstance();

    @Override
    public void handle(Long userId, Long chatId, String text, TelegramBot bot) {
        if (text == null || text.trim().isEmpty()) {
            bot.sendMessage(chatId, "Имя напоминания не может быть пустым. Попробуйте снова или введите /cancel.");
            return;
        }

        String cleanName = text.trim();

        try {
            reminderService.removeReminder(userId, cleanName);
            bot.sendMessage(chatId, "Напоминание \"" + cleanName + "\" удалено.");
        } catch (SQLException e) {
            e.printStackTrace();
            bot.sendMessage(chatId, "Ошибка при удалении напоминания. Возможно, оно не существует или произошла ошибка базы данных.");
        } finally {
            fsm.resetState(userId);
        }
    }
}