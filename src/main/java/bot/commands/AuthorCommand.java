package bot.commands;

import org.telegram.telegrambots.meta.api.objects.Message;
import bot.TelegramBot;

public class AuthorCommand implements Command {

    @Override
    public String getCommandName() {
        return "author";
    }

    @Override
    public String getDescription() {
        return "Показать информацию об авторе";
    }

    @Override
    public String getUsage() {
        return "/author";
    }

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        String authorInfo = "Автор: Дмитрий Екимов";
        bot.sendMessage(message.getChatId(), authorInfo);
    }
}