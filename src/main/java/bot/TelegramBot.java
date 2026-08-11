package bot;

import bot.commands.Command;
import bot.fsm.BotFSM;
import bot.fsm.UserState;
import bot.note.NoteService;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramBot extends TelegramLongPollingBot {

    private final String botToken = System.getenv("TELEGRAM_BOT_TOKEN");
    private final CommandRegistry commandRegistry;
    private final NoteService noteService = NoteService.INSTANCE;
    private final BotFSM fsm = BotFSM.INSTANCE;

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
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();

        if (!message.hasText()) {
            return; // Игнорируем не-текстовые сообщения
        }

        String text = message.getText().trim();

        // "/cancel"
        if (text.equals("/cancel")) {
            fsm.resetState(userId); // Сбрасываем в DEFAULT
            sendMessage(chatId, "Операция отменена. Вы возвращены в главное меню.");
            return;
        }

        // Получаем текущее состояние пользователя
        UserState state = fsm.getState(userId);

        // Обрабатываем ввод в зависимости от состояния
        switch (state) {
            case DEFAULT:
                handleDefaultState(chatId, text, message);
                break;
            case AWAITING_NOTE_NAME_ADD:
                handleAwaitingNoteNameAdd(userId, chatId, text);
                break;
            case AWAITING_NOTE_TEXT_ADD:
                handleAwaitingNoteTextAdd(userId, chatId, text);
                break;
            case AWAITING_NOTE_NAME_EDIT:
                handleAwaitingNoteNameEdit(userId, chatId, text);
                break;
            case AWAITING_NOTE_TEXT_EDIT:
                handleAwaitingNoteTextEdit(userId, chatId, text);
                break;
            case AWAITING_NOTE_NAME_SHOW:
                handleAwaitingNoteNameShow(userId, chatId, text);
                break;
            case AWAITING_NOTE_NAME_REMOVE:
                handleAwaitingNoteNameRemove(userId, chatId, text);
                break;
        }
    }
    
    private void handleDefaultState(Long chatId, String text, Message message) {
        if (text.startsWith("/")) {
            String[] parts = text.split("\\s+", 2);
            String cmdName = parts[0].substring(1); // убираем "/"
            String[] args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];

            Command command = commandRegistry.getCommand(cmdName);
            if (command != null) {
                command.execute(this, message, args);
            }  
            else {
                sendMessage(chatId, "Неизвестная команда. Введите /help для списка команд.");
            }
        } else {
            sendMessage(chatId, "Введите команду, например, /help.");
        }
    }
    // --- FSM ---

    private void handleAwaitingNoteNameAdd(Long userId, Long chatId, String text) {
        if (text == null || text.trim().isEmpty()) {
            sendMessage(chatId, "Имя заметки не может быть пустым. Попробуйте снова: /addNote");
            fsm.resetState(userId);
            return;
        }

        String cleanName = text.trim();
        
        // Проверяем, существует ли заметка с таким именем
        try {
            List<String> userNotes = noteService.getUserNotes(userId);
            if (userNotes.contains(cleanName)) {
                sendMessage(chatId, "Заметка с именем \"" + cleanName + "\" уже существует. Введите другое имя.");
                // Не сбрасываем состояние, пользователь вводит имя снова
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendMessage(chatId, "Ошибка при проверке существования заметки. Попробуйте позже.");
            fsm.resetState(userId);
            return;
        }
        
        sendMessage(chatId, "Введите текст для заметки \"" + cleanName + "\".");
        fsm.setState(userId, UserState.AWAITING_NOTE_TEXT_ADD);
        tempNoteNameStorage.put(userId, cleanName);
    }

    private void handleAwaitingNoteTextAdd(Long userId, Long chatId, String text) {
        String noteName = tempNoteNameStorage.remove(userId);

        if (text == null || text.trim().isEmpty()) {
            sendMessage(chatId, "Текст заметки не может быть пустым. Операция отменена.");
            fsm.resetState(userId);
            return;
        }

        try {
            noteService.addNoteToDB(userId, noteName, text.trim());
            sendMessage(chatId, "Заметка \"" + noteName + "\" добавлена!");
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(chatId, "Ошибка добавления заметки. Попробуйте позже.");
        }
        fsm.resetState(userId);
    }

    private void handleAwaitingNoteNameEdit(Long userId, Long chatId, String text) {
        if (text == null || text.trim().isEmpty()) {
            sendMessage(chatId, "Имя заметки не может быть пустым. Попробуйте снова: /editNote");
            fsm.resetState(userId);
            return;
        }

        String cleanName = text.trim();
        sendMessage(chatId, "Введите отредактируемый текст для заметки \"" + cleanName + "\".");
        fsm.setState(userId, UserState.AWAITING_NOTE_TEXT_EDIT);
        tempNoteNameStorage.put(userId, cleanName);
    }

    private void handleAwaitingNoteTextEdit(Long userId, Long chatId, String text) {
        String noteName = tempNoteNameStorage.remove(userId);

        if (text == null || text.trim().isEmpty()) {
            sendMessage(chatId, "Текст заметки не может быть пустым. Операция отменена.");
            fsm.resetState(userId);
            return;
        }

        try {
            noteService.removeNoteFromDB(userId, noteName);
            noteService.addNoteToDB(userId, noteName, text.trim());
            sendMessage(chatId, "Заметка \"" + noteName + "\" успешно обновлена!");
        } catch (Exception e) {
            e.printStackTrace();
            sendMessage(chatId, "Ошибка при редактировании заметки. Попробуйте позже.");
        }
        fsm.resetState(userId);
    }

    private void handleAwaitingNoteNameShow(Long userId, Long chatId, String text) {
        if (text == null || text.trim().isEmpty()) {
            sendMessage(chatId, "Имя заметки не может быть пустым. Попробуйте снова: /showNote");
            fsm.resetState(userId);
            return;
        }

        String cleanName = text.trim();
        try {
            String noteText = noteService.getNote(userId, cleanName);
            if (noteText != null) {
                sendMessage(chatId, cleanName + "\n" + noteText);
            } else {
                sendMessage(chatId, "Заметка \"" + cleanName + "\" не найдена.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendMessage(chatId, "Не удалось получить заметку. Попробуйте позже.");
        }
        fsm.resetState(userId);
    }

    private void handleAwaitingNoteNameRemove(Long userId, Long chatId, String text) {
        if (text == null || text.trim().isEmpty()) {
            sendMessage(chatId, "Имя заметки не может быть пустым. Попробуйте снова: /removeNote");
            fsm.resetState(userId);
            return;
        }

        String cleanName = text.trim();

        try {
            String noteText = noteService.getNote(userId, cleanName);
            if (noteText != null) {
                noteService.removeNoteFromDB(userId, cleanName);
                sendMessage(chatId, "Заметка \"" + cleanName + "\" удалена!");
            } else {
                sendMessage(chatId, "Заметка \"" + cleanName + "\" не найдена.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            sendMessage(chatId, "Не удалось удалить заметку. Попробуйте позже.");
        }
        fsm.resetState(userId);
    }
    
    // Временное хранилище для передачи данных между состояниями
    private final Map<Long, String> tempNoteNameStorage = new HashMap<>();
    
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