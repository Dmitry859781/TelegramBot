package bot.commands.reminders;

import bot.TelegramBot;
import bot.commands.Command;
import bot.fsm.BotFSM;
import bot.fsm.UserState;
import org.telegram.telegrambots.meta.api.objects.Message;

public class AddRecurringReminderCommand implements Command {

    private final BotFSM fsm = BotFSM.getInstance();

    @Override
    public String getCommandName() {
        return "addRecurringReminder";
    }

    @Override
    public String getDescription() {
        return "Добавить повторяющееся напоминание";
    }

    @Override
    public String getUsage() {
        return "/addRecurringReminder";
    }

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();

        bot.sendMessage(chatId, "Введите имя напоминания:");
        fsm.setState(userId, UserState.AWAITING_REMINDER_NAME_TO_ADD);
        fsm.setTempData(userId, "command", "addRecurringReminder");
    }
}