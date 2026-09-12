package bot.handlers.notes;

import bot.AbstractBotTest;
import bot.fsm.BotFSM;
import bot.note.NoteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.sql.SQLException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HandleAwaitingNoteNameToShowTest extends AbstractBotTest {

    @Mock
    private NoteService noteService;

    @Mock
    private BotFSM fsm;

    @Test
    @DisplayName("Тест handle: успешный вывод существующей заметки")
    void testHandle_Success() throws SQLException {
        // Given заметка существует
        when(noteService.getNote(userId, "my_note")).thenReturn("Текст моей заметки");

        try (MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingNoteNameToShow handler = new HandleAwaitingNoteNameToShow();

            // When
            handler.handle(userId, chatId, "my_note", bot);

            // Then
            verify(noteService).getNote(userId, "my_note");
            verify(bot).sendMessage(eq(chatId), eq("my_note\nТекст моей заметки"));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: заметка не найдена")
    void testHandle_NoteNotFound() throws SQLException {
        // Given заметка не существует
        when(noteService.getNote(userId, "missing_note")).thenReturn(null);

        try (MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingNoteNameToShow handler = new HandleAwaitingNoteNameToShow();

            // When
            handler.handle(userId, chatId, "missing_note", bot);

            // Then
            verify(noteService).getNote(userId, "missing_note");
            verify(bot).sendMessage(eq(chatId), eq("Заметка \"missing_note\" не найдена."));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: пустой или пробельный ввод")
    void testHandle_EmptyInput() throws SQLException {
        try (MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingNoteNameToShow handler = new HandleAwaitingNoteNameToShow();

            // When
            handler.handle(userId, chatId, "   ", bot);

            // Then
            // Проверяем, что к базе данных не обращались
            verify(noteService, never()).getNote(anyLong(), anyString());
            verify(bot).sendMessage(eq(chatId), eq("Имя заметки не может быть пустым. Попробуйте снова или введите /cancel"));
            verify(fsm, never()).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: ошибка базы данных при получении заметки")
    void testHandle_DatabaseError() throws SQLException {
        // Given база данных выбрасывает исключение
        when(noteService.getNote(userId, "error_note")).thenThrow(new SQLException("DB connection failed"));

        try (MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingNoteNameToShow handler = new HandleAwaitingNoteNameToShow();

            // When
            handler.handle(userId, chatId, "error_note", bot);

            // Then
            verify(noteService).getNote(userId, "error_note");
            verify(bot).sendMessage(eq(chatId), eq("Не удалось получить заметку. Попробуйте позже."));
            verify(fsm).resetState(userId);
        }
    }
}