package bot.handlers.notes;

import bot.AbstractBotTest;
import bot.fsm.BotFSM;
import bot.fsm.UserState;
import bot.note.NoteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.sql.SQLException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HandleAwaitingNoteNameToEditTest extends AbstractBotTest {

    @Mock
    private NoteService noteService;

    @Mock
    private BotFSM fsm;

    @Test
    @DisplayName("Тест handle: успешный поиск заметки и переход к редактированию имени")
    void testHandle_Success() throws SQLException {
        // Given заметка существует
        when(noteService.getNote(userId, "my_note")).thenReturn("какой-то текст заметки");

        try (MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingNoteNameToEdit handler = new HandleAwaitingNoteNameToEdit();

            // When
            handler.handle(userId, chatId, "my_note", bot);

            // Then
            verify(noteService).getNote(userId, "my_note");
            verify(fsm).setTempData(userId, "oldNoteName", "my_note");
            verify(fsm).setTempData(userId, "currentNoteText", "какой-то текст заметки");
            verify(fsm).setState(userId, UserState.AWAITING_NOTE_NEW_NAME);
            verify(bot).sendMessage(eq(chatId), eq("Текущее имя: \"my_note\"\n\nВведите новое имя заметки (или отправьте `-`, чтобы оставить без изменений):"));
        }
    }

    @Test
    @DisplayName("Тест handle: пустой или пробельный ввод")
    void testHandle_EmptyInput() throws SQLException {
        try (MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingNoteNameToEdit handler = new HandleAwaitingNoteNameToEdit();

            // When
            handler.handle(userId, chatId, "   ", bot);

            // Then
            // Проверяем, что к базе данных даже не обращались (ранний возврат)
            verify(noteService, never()).getNote(anyLong(), anyString());
            verify(bot).sendMessage(eq(chatId), eq("Имя заметки не может быть пустым. Попробуйте снова или введите /cancel."));
            verify(fsm, never()).setTempData(anyLong(), anyString(), any());
            verify(fsm, never()).setState(anyLong(), any());
        }
    }

    @Test
    @DisplayName("Тест handle: заметка с таким именем не найдена")
    void testHandle_NoteNotFound() throws SQLException {
        // Given заметка не существует
        when(noteService.getNote(userId, "missing_note")).thenReturn(null);

        try (MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingNoteNameToEdit handler = new HandleAwaitingNoteNameToEdit();

            // When
            handler.handle(userId, chatId, "missing_note", bot);

            // Then
            verify(noteService).getNote(userId, "missing_note");
            verify(bot).sendMessage(eq(chatId), eq("Заметка с именем \"missing_note\" не найдена. Проверьте имя или введите /cancel."));
            // Состояние и временные данные не должны быть изменены
            verify(fsm, never()).setTempData(anyLong(), anyString(), any());
            verify(fsm, never()).setState(anyLong(), any());
        }
    }

    @Test
    @DisplayName("Тест handle: ошибка базы данных при поиске заметки")
    void testHandle_DatabaseError() throws SQLException {
        // Given база данных выбрасывает исключение
        when(noteService.getNote(userId, "error_note")).thenThrow(new SQLException("DB connection failed"));

        try (MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingNoteNameToEdit handler = new HandleAwaitingNoteNameToEdit();

            // When
            handler.handle(userId, chatId, "error_note", bot);

            // Then
            verify(noteService).getNote(userId, "error_note");
            verify(bot).sendMessage(eq(chatId), eq("Ошибка при поиске заметки. Попробуйте позже."));
            // При критической ошибке состояние должно быть сброшено
            verify(fsm).resetState(userId);
        }
    }
}