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
import java.util.List;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class HandleAwaitingNoteNameToAddTest extends AbstractBotTest {

    @Mock
    private NoteService noteService;

    @Mock
    private BotFSM fsm;

    @Test
    @DisplayName("Тест handle: успешное добавление нового имени заметки")
    void testHandle_Success() throws SQLException {
        // Given
        when(noteService.getUserNotes(userId)).thenReturn(List.of("другая_заметка"));

        try (MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingNoteNameToAdd handler = new HandleAwaitingNoteNameToAdd();

            // When
            handler.handle(userId, chatId, "новая_заметка", bot);

            // Then
            verify(noteService).getUserNotes(userId);
            verify(bot).sendMessage(eq(chatId), eq("Введите текст для заметки \"новая_заметка\"."));
            verify(fsm).setState(userId, UserState.AWAITING_NOTE_TEXT);
            verify(fsm).setTempData(userId, "noteName", "новая_заметка");
        }
    }

    @Test
    @DisplayName("Тест handle: пустой или пробельный ввод")
    void testHandle_EmptyInput() throws SQLException {
        try (MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingNoteNameToAdd handler = new HandleAwaitingNoteNameToAdd();

            // When
            handler.handle(userId, chatId, "   ", bot);

            // Then
            // Проверяем, что к базе данных даже не обращались
            verify(noteService, never()).getUserNotes(anyLong());
            verify(bot).sendMessage(eq(chatId), eq("Имя заметки не может быть пустым. Попробуйте снова или введите /cancel"));
            verify(fsm, never()).setState(anyLong(), any());
        }
    }

    @Test
    @DisplayName("Тест handle: заметка с таким именем уже существует")
    void testHandle_NameAlreadyExists() throws SQLException {
        // Given заметка с таким именем уже есть в списке
        when(noteService.getUserNotes(userId)).thenReturn(List.of("существующая_заметка"));

        try (MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingNoteNameToAdd handler = new HandleAwaitingNoteNameToAdd();

            // When
            handler.handle(userId, chatId, "существующая_заметка", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Заметка с именем \"существующая_заметка\" уже существует. Введите другое имя или /cancel."));
            // Состояние не должно измениться
            verify(fsm, never()).setState(anyLong(), any());
            verify(fsm, never()).setTempData(anyLong(), anyString(), any());
        }
    }

    @Test
    @DisplayName("Тест handle: ошибка базы данных при проверке имени")
    void testHandle_DatabaseError() throws SQLException {
        // Given база данных выбрасывает исключение
        when(noteService.getUserNotes(userId)).thenThrow(new SQLException("DB connection failed"));

        try (MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingNoteNameToAdd handler = new HandleAwaitingNoteNameToAdd();

            // When
            handler.handle(userId, chatId, "новая_заметка", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Ошибка при проверке существования заметки. Попробуйте позже."));
            // При критической ошибке состояние должно быть сброшено
            verify(fsm).resetState(userId);
        }
    }
}