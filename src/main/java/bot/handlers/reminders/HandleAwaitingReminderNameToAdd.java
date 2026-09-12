package bot.handlers.reminders;

import bot.TelegramBot;
import bot.fsm.BotFSM;
import bot.fsm.UserState;
import bot.handlers.StateHandler;

public class HandleAwaitingReminderNameToAdd implements StateHandler{
	private final BotFSM fsm = BotFSM.getInstance();
	
	public void handle(Long userId, Long chatId, String text, TelegramBot bot) {
        if (text == null || text.trim().isEmpty()) {
            bot.sendMessage(chatId, "Имя напоминания не может быть пустым. Попробуйте снова или введите /cancel.");
            return;
        }
        
        String cleanName = text.trim();
        String command = (String) fsm.getTempData(userId, "command");

        if ("addOnceReminder".equals(command)) {
            bot.sendMessage(chatId, "Введите дату и время для разового напоминания (формат: DD-MM-YYYY HH:ММ или DD-MM HH:ММ для текущего года).");
            fsm.setState(userId, UserState.AWAITING_ONCE_DATE);
        } else if ("addRecurringReminder".equals(command)) {
            bot.sendMessage(chatId, "Введите дни недели и время для повторяющегося напоминания (например: ПН-ПТ 09:00, MON 18:51).");
            fsm.setState(userId, UserState.AWAITING_RECURRING_DAYS);
        }  else {
            bot.sendMessage(chatId, "Произошла внутренняя ошибка инициализации. Пожалуйста, начните заново.");
            fsm.resetState(userId);
            return;
        }
		fsm.setTempData(userId, "reminderName", cleanName);
	}
}
