package bot.commands;

import bot.TelegramBot;
import bot.timezone.UserTimezoneService;

import org.telegram.telegrambots.meta.api.objects.Message;

import java.sql.SQLException;
import java.util.Map;

public class StartCommand implements Command {

    private final Map<String, Command> commandRegistry;
    private final UserTimezoneService timezoneService = UserTimezoneService.getInstance(); 

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
    	Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();

        try {
            Integer offset = timezoneService.getTimezone(userId);
            
            if (offset == null) {
                bot.sendMessage(chatId, "У вас не установлена временная зона, используйте команду /setOrEditTimezone для установки.");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            bot.sendMessage(chatId, "Ошибка при проверке настроек. Попробуйте позже.");
            return;
        }
        StringBuilder response = new StringBuilder("Доступные команды:\n");
        for (Command cmd : commandRegistry.values()) {
            response.append("/").append(cmd.getCommandName())
                    .append(" — ").append(cmd.getDescription()).append("\n");
        }
        bot.sendMessage(message.getChatId(), response.toString());
    }
}