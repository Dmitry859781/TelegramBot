package bot.commands;

import bot.TelegramBot;
import bot.fsm.BotFSM;
import org.telegram.telegrambots.meta.api.objects.Message;

public class CancelCommand implements Command {

    private final BotFSM fsm;

    public CancelCommand(BotFSM fsm) {
        this.fsm = fsm;
    }

    @Override
    public String getCommandName() {
        return "cancel";
    }

    @Override
    public String getDescription() {
        return "Отменить текущую операцию и вернуться в главное меню";
    }

    @Override
    public String getUsage() {
        return "/cancel";
    }

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();

        fsm.resetState(userId);
        bot.sendMessage(chatId, "Операция отменена. Вы возвращены в главное меню.");
    }
}