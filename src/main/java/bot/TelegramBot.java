package bot;

import bot.commands.Command;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramBot extends TelegramLongPollingBot {

    private final String botToken = System.getenv("TELEGRAM_BOT_TOKEN");
    private final CommandRegistry commandRegistry;

    public TelegramBot(CommandRegistry registry) {
        this.commandRegistry = registry;
    }

    @Override
    public String getBotUsername() {
        return "Unterrichtung_bot";
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) return;

        Message message = update.getMessage();
        Long chatId = message.getChatId();

        if (!message.hasText()) {
            return; // Игнорируем не-текстовые сообщения
        }

        String text = message.getText().trim();

        if (text.startsWith("/")) {
            String[] parts = text.split("\\s+", 2);
            String cmdName = parts[0].substring(1); // убираем "/"
            String[] args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];

            Command command = commandRegistry.getCommand(cmdName);
            if (command != null) {
                command.execute(this, message, args);
            } else {
                sendMessage(chatId, "Неизвестная команда. Введите /help для списка команд.");
            }
        }
    }

    public void sendMessage(Long chatId, String text) {
        org.telegram.telegrambots.meta.api.methods.send.SendMessage msg =
                org.telegram.telegrambots.meta.api.methods.send.SendMessage.builder()
                        .chatId(chatId.toString())
                        .text(text)
                        .build();
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}