package bot.commands.notes;

import bot.TelegramBot;
import bot.commands.Command;
import bot.fsm.BotFSM;
import bot.fsm.UserState;
import org.telegram.telegrambots.meta.api.objects.Message;

public class AddNoteCommand implements Command {

    private final BotFSM fsm = BotFSM.getInstance();

    public AddNoteCommand() {
    }

    @Override
    public String getCommandName() {
        return "addNote";
    }

    @Override
    public String getDescription() {
        return "Добавить заметку";
    }

    @Override
    public String getUsage() {
        return "/addNote";
    }

    @Override
    public void execute(TelegramBot bot, Message message, String[] args) {
        Long userId = message.getFrom().getId();

        bot.sendMessage(message.getChatId(), "Введите имя добавляемой заметки.");
        fsm.setState(userId, UserState.AWAITING_NOTE_NAME_TO_ADD);
        fsm.setTempData(userId, "command", "addNote");
    }
}