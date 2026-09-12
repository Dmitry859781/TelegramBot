package bot.commands.timezone;

import bot.TelegramBot;
import bot.commands.Command;
import bot.fsm.BotFSM;
import bot.fsm.UserState;
import bot.timezone.UserTimezoneService;

import org.telegram.telegrambots.meta.api.objects.Message;

public class SetOrEditTimezoneCommand implements Command {

    private final UserTimezoneService timezoneService = UserTimezoneService.getInstance();
    private final BotFSM fsm = BotFSM.getInstance();

    @Override
    public String getCommandName() {
        return "setOrEditTimezone";
    }

    @Override
    public String getDescription() {
        return "Установить или изменить ваш часовой пояс";
    }

    @Override
    public String getUsage() {
        return "/setOrEditTimezone";
    }

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();

        try {
            Integer currentTimezone = timezoneService.getTimezone(userId);
            StringBuilder prompt = new StringBuilder();

            if (currentTimezone != null) {
                String sign = currentTimezone >= 0 ? "+" : "";
                prompt.append("Ваш текущий часовой пояс: UTC").append(sign).append(currentTimezone).append("\n\n");
            }

            prompt.append(
                "Пожалуйста, укажите ваш часовой пояс как смещение от UTC.\n\n" +
                "Примеры:\n" +
                "Москва → +3\n" +
                "Екатеринбург → +5\n" +
                "Владивосток → +10\n" +
                "Лондон → +0\n" +
                "Нью-Йорк → -5\n\n" +
                "Просто отправьте число со знаком: +3, -4, +0 и т.д."
            );

            bot.sendMessage(chatId, prompt.toString());

            // Устанавливаем состояние ожидания ввода смещения
            fsm.setState(userId, UserState.AWAITING_TIMEZONE_OFFSET);
            fsm.setTempData(userId, "command", "setOrEditTimezone");

        } catch (Exception e) {
            e.printStackTrace();
            bot.sendMessage(chatId, "Ошибка при загрузке часового пояса. Попробуйте позже.");
        }
    }
}