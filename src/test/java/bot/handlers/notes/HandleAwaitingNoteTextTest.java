package bot.handlers.notes;

import bot.AbstractBotTest;
import bot.fsm.BotFSM;
import bot.note.NoteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.sql.SQLException;

class HandleAwaitingNoteTextTest extends AbstractBotTest {

    @Mock
    private NoteService noteService;

    @Mock
    private BotFSM fsm;

    @Test
    @DisplayName("Тест handle: успешное добавление заметки в базу данных")
    void testHandle_Success() throws SQLException {
        // Given в FSM есть имя заметки
        when(fsm.getTempData(userId, "noteName")).thenReturn("my_new_note");

        try (MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingNoteText handler = new HandleAwaitingNoteText();

            // When
            handler.handle(userId, chatId, "  Текст моей новой заметки  ", bot);

            // Then
            verify(fsm).getTempData(userId, "noteName");
            verify(fsm).clearTempData(userId, "noteName");
            // Проверяем, что текст был обрезан (trim) перед сохранением
            verify(noteService).addNoteToDB(userId, "my_new_note", "Текст моей новой заметки");
            verify(bot).sendMessage(eq(chatId), eq("Заметка \"my_new_note\" добавлена!"));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: пустой или пробельный ввод текста")
    void testHandle_EmptyInput() throws SQLException {
        // Given в FSM есть имя заметки, но пользователь вводит пробелы
        when(fsm.getTempData(userId, "noteName")).thenReturn("my_note");

        try (MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingNoteText handler = new HandleAwaitingNoteText();

            // When
            handler.handle(userId, chatId, "   ", bot);

            // Then
            verify(fsm).getTempData(userId, "noteName");
            verify(fsm).clearTempData(userId, "noteName");
            verify(bot).sendMessage(eq(chatId), eq("Текст заметки не может быть пустым. Попробуйте снова или введите /cancel"));
            
            // Проверяем, что из-за раннего return база данных не трогалась
            verify(noteService, never()).addNoteToDB(anyLong(), anyString(), anyString());
            verify(fsm, never()).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: ошибка при добавлении заметки в базу данных")
    void testHandle_DatabaseError() throws SQLException {
        // Given в FSM есть имя заметки, но БД выбрасывает исключение
        when(fsm.getTempData(userId, "noteName")).thenReturn("error_note");
        doThrow(new RuntimeException("DB connection failed"))
                .when(noteService).addNoteToDB(anyLong(), anyString(), anyString());

        try (MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingNoteText handler = new HandleAwaitingNoteText();

            // When
            handler.handle(userId, chatId, "Текст заметки", bot);

            // Then
            verify(fsm).getTempData(userId, "noteName");
            verify(fsm).clearTempData(userId, "noteName");
            verify(noteService).addNoteToDB(userId, "error_note", "Текст заметки");
            verify(bot).sendMessage(eq(chatId), eq("Ошибка добавления заметки. Попробуйте позже."));
            verify(fsm).resetState(userId);
        }
    }
}