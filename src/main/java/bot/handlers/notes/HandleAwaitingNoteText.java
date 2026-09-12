package bot.handlers.notes;

import bot.TelegramBot;
import bot.fsm.BotFSM;
import bot.handlers.StateHandler;
import bot.note.NoteService;

public class HandleAwaitingNoteText implements StateHandler{
	private final NoteService noteService = NoteService.getInstance();
	private final BotFSM fsm = BotFSM.getInstance();
	
	public void handle(Long userId, Long chatId, String text, TelegramBot bot) {
    	String noteName = (String) fsm.getTempData(userId, "noteName");
    	fsm.clearTempData(userId, "noteName"); 
    	
        if (text == null || text.trim().isEmpty()) {
            bot.sendMessage(chatId, "Текст заметки не может быть пустым. Попробуйте снова или введите /cancel");
            return;
        }

        try {
            noteService.addNoteToDB(userId, noteName, text.trim());
            bot.sendMessage(chatId, "Заметка \"" + noteName + "\" добавлена!");
        } catch (Exception e) {
            e.printStackTrace();
            bot.sendMessage(chatId, "Ошибка добавления заметки. Попробуйте позже.");
        } finally {
        fsm.resetState(userId);
        }
    }
}
