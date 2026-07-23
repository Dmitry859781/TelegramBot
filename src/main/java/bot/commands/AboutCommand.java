package bot.commands;

import org.telegram.telegrambots.meta.api.objects.Message;
import bot.TelegramBot;

public class AboutCommand implements Command {

    @Override
    public String getCommandName() {
        return "about";
    }

    @Override
    public String getDescription() {
        return "Показать информацию о боте";
    }

    @Override
    public String getUsage() {
        return "/about";
    }

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        String info = "Этот бот предназначен для напоминаний и других полезных функций.\n" +
                "Он позволяет создавать, управлять и получать уведомления о событиях.";
        bot.sendMessage(message.getChatId(), info);
    }
}