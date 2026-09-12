package bot.handlers.reminders;

import bot.TelegramBot;
import bot.fsm.BotFSM;
import bot.handlers.StateHandler;
import bot.reminder.Reminder;
import bot.reminder.ReminderService;

import java.sql.SQLException;

public class HandleAwaitingReminderNameToShow implements StateHandler {

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
            Reminder reminder = reminderService.getReminder(userId, cleanName);
            if (reminder != null) {
                bot.sendMessage(chatId, reminder.getName());
            } else {
                bot.sendMessage(chatId, "Напоминание \"" + cleanName + "\" не найдено.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            bot.sendMessage(chatId, "Ошибка при получении напоминания из базы данных.");
        } finally {
            fsm.resetState(userId);
        }
    }
}