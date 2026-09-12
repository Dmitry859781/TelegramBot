package bot;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        CommandRegistry registry = new CommandRegistry();

        TelegramBot bot = new TelegramBot(registry);

        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(bot);
            System.out.println("Бот запущен!");
            Thread.currentThread().join();
        } catch (TelegramApiException e) {
            System.err.println("Ошибка при запуске бота: " + e.getMessage());
            e.printStackTrace();
        } finally {
        	bot.onDestroy();
        }
    }
}