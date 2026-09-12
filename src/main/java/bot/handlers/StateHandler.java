package bot.handlers;

import bot.TelegramBot;

public interface StateHandler {
	void handle(Long userId, Long chatId, String text, TelegramBot bot);
}
