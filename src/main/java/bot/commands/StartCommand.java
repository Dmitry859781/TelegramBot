package bot.commands;

import bot.TelegramBot;
import bot.commands.Command;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Map;

public class StartCommand implements Command {

    private final Map<String, Command> commandRegistry;

    public StartCommand(Map<String, Command> commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    @Override
    public String getCommandName() {
        return "start";
    }

    @Override
    public String getDescription() {
        return "Показать список команд";
    }

    @Override
    public String getUsage() {
        return "/start";
    }

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        StringBuilder response = new StringBuilder("Доступные команды:\n");
        for (Command cmd : commandRegistry.values()) {
            response.append("<code>/").append(cmd.getCommandName()).append("</code> - ").append(cmd.getDescription()).append("\n");
        }
        bot.sendMessage(message.getChatId(), response.toString());
    }
}