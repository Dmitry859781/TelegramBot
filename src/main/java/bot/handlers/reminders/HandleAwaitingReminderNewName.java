package bot.handlers.reminders;

import bot.TelegramBot;
import bot.fsm.BotFSM;
import bot.fsm.UserState;
import bot.handlers.StateHandler;

public class HandleAwaitingReminderNewName implements StateHandler {

    private final BotFSM fsm = BotFSM.getInstance();

    @Override
    public void handle(Long userId, Long chatId, String text, TelegramBot bot) {
        if (text == null || text.trim().isEmpty()) {
            bot.sendMessage(chatId, "Ввод не может быть пустым. Попробуйте снова или введите /cancel.");
            return;
        }
        
        String input = text.trim();
        String oldName = (String) fsm.getTempData(userId, "oldReminderName");
        String reminderType = (String) fsm.getTempData(userId, "reminderType");

        // Если "-", оставляем старое имя, иначе берем новое
        String newName = "-".equals(input) ? oldName : input;
        
        // Сохраняем итоговое имя для следующего шага
        fsm.setTempData(userId, "newReminderName", newName);

        // Переходим к редактированию времени
        fsm.setState(userId, UserState.AWAITING_REMINDER_NEW_TIME);
        
        if ("ONCE".equals(reminderType)) {
            bot.sendMessage(chatId, "Введите новую дату и время (например: 16-08-2026 15:30) или `-`, чтобы оставить старое время:");
        } else if ("RECURRING".equals(reminderType)){
            bot.sendMessage(chatId, "Введите новые дни и время (например: ПН-ПТ 09:00) или `-`, чтобы оставить старое расписание:");
        }  else {
            bot.sendMessage(chatId, "Произошла внутренняя ошибка инициализации. Пожалуйста, начните заново.");
            fsm.resetState(userId);
            return;
        }
    }
}