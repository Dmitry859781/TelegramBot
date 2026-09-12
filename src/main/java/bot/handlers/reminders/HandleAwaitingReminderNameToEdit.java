package bot.handlers.reminders;

import bot.TelegramBot;
import bot.fsm.BotFSM;
import bot.fsm.UserState;
import bot.handlers.StateHandler;
import bot.reminder.Reminder;
import bot.reminder.ReminderService;

import java.sql.SQLException;

public class HandleAwaitingReminderNameToEdit implements StateHandler {

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
            if (reminder == null) {
                bot.sendMessage(chatId, "Напоминание с именем \"" + cleanName + "\" не найдено. Проверьте имя или введите /cancel.");
                return;
            }

            // Сохраняем текущие данные для использования при вводе "-"
            fsm.setTempData(userId, "oldReminderName", cleanName);
            fsm.setTempData(userId, "oldPropertiesJson", reminder.getPropertiesJson());
            fsm.setTempData(userId, "reminderType", reminder.getType().name());

            // Переходим к редактированию имени
            fsm.setState(userId, UserState.AWAITING_REMINDER_NEW_NAME);
            bot.sendMessage(chatId, "Текущее имя: \"" + cleanName + "\"\n\nВведите новое имя напоминания (или отправьте `-`, чтобы оставить без изменений):");

        } catch (SQLException e) {
            e.printStackTrace();
            bot.sendMessage(chatId, "Ошибка при поиске напоминания. Попробуйте позже.");
            fsm.resetState(userId);
        }
    }
}