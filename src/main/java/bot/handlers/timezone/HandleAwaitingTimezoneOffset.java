package bot.handlers.timezone;

import bot.TelegramBot;
import bot.fsm.BotFSM;
import bot.handlers.StateHandler;
import bot.timezone.UserTimezoneService;

public class HandleAwaitingTimezoneOffset implements StateHandler {

    private final BotFSM fsm = BotFSM.getInstance();
    private final UserTimezoneService timezoneService = UserTimezoneService.getInstance();

    @Override
    public void handle(Long userId, Long chatId, String text, TelegramBot bot) {
        if (text == null || text.trim().isEmpty()) {
            bot.sendMessage(chatId, "Смещение не может быть пустым. Попробуйте снова: /setOrEditTimezone");
            fsm.resetState(userId);
            return;
        }

        String raw = text.trim();
        try {
            int offset = parseOffsetAsInt(raw);
            if (offset < -12 || offset > 14) {
                bot.sendMessage(chatId, "Допустимый диапазон: от -12 до +14. Попробуйте снова: /setOrEditTimezone");
                fsm.resetState(userId);
                return;
            }

            timezoneService.saveTimezoneOffset(userId, offset);
            String sign = offset >= 0 ? "+" : "";
            bot.sendMessage(chatId, "Часовой пояс установлен: UTC" + sign + offset);
        } catch (Exception e) {
            bot.sendMessage(chatId,
                "Неверный формат.\n" +
                "Пожалуйста, введите смещение от UTC в формате:\n" +
                "+3, -5, +0 и т.д.\n" +
                "Попробуйте снова"
            );
        }
        fsm.resetState(userId);
    }

    private int parseOffsetAsInt(String input) {
        String clean = input.trim().replaceAll("[^0-9+-]", "");
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("Пустой ввод");
        }
        // Обрабатываем +0, -0
        if (clean.equals("+0") || clean.equals("-0") || clean.equals("0")) {
            return 0;
        }
        return Integer.parseInt(clean);
    }
}