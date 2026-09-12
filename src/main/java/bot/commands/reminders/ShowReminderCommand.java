package bot.commands.reminders;

import bot.TelegramBot;
import bot.commands.Command;
import bot.fsm.BotFSM;
import bot.fsm.UserState;
import bot.reminder.ReminderService;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;

public class ShowReminderCommand implements Command {

    private final ReminderService reminderService = ReminderService.getInstance();
    private final BotFSM fsm = BotFSM.getInstance();

    @Override
    public String getCommandName() {
        return "showReminder";
    }

    @Override
    public String getDescription() {
        return "Показать напоминание";
    }

    @Override
    public String getUsage() {
        return "/showReminder";
    }

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();

        try {
            List<String> names = reminderService.getUserReminderNames(userId, null);
            if (names.isEmpty()) {
                bot.sendMessage(chatId, "У вас нет напоминаний. Добавьте сначала через /addOnceReminder или /addRecurringReminder.");
                return;
            }

            StringBuilder sb = new StringBuilder("Какое напоминание показать?\n");
            for (int i = 0; i < names.size(); i++) {
                sb.append(i + 1).append(". ").append(names.get(i)).append("\n");
            }
            bot.sendMessage(chatId, sb.toString());
            bot.sendMessage(chatId, "Введите имя напоминания:");
            fsm.setState(userId, UserState.AWAITING_REMINDER_NAME_TO_SHOW);
            fsm.setTempData(userId, "command", "showReminder");
        } catch (Exception e) {
            bot.sendMessage(chatId, "Ошибка при получении списка напоминаний. Попробуйте позже.");
        }
    }
}