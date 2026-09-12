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

class HandleAwaitingNoteNewTextTest extends AbstractBotTest {

    @Mock
    private NoteService noteService;

    @Mock
    private BotFSM fsm;

    @Test
    @DisplayName("Тест handle: успешное обновление с новым текстом")
    void testHandle_SuccessWithNewText() throws SQLException {
        // Given данные есть в FSM
        when(fsm.getTempData(userId, "oldNoteName")).thenReturn("old_name");
        when(fsm.getTempData(userId, "newNoteName")).thenReturn("new_name");
        when(fsm.getTempData(userId, "currentNoteText")).thenReturn("старый текст");

        try (MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingNoteNewText handler = new HandleAwaitingNoteNewText();

            // When
            handler.handle(userId, chatId, "  новый текст  ", bot);

            // Then
            verify(noteService).updateNote(userId, "old_name", "new_name", "новый текст");
            verify(bot).sendMessage(eq(chatId), eq("Заметка успешно обновлена!\nНовое имя: \"new_name\""));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: успешное обновление с сохранением старого текста (ввод '-')")
    void testHandle_SuccessKeepOldText() throws SQLException {
        // Given данные есть в FSM
        when(fsm.getTempData(userId, "oldNoteName")).thenReturn("old_name");
        when(fsm.getTempData(userId, "newNoteName")).thenReturn("new_name");
        when(fsm.getTempData(userId, "currentNoteText")).thenReturn("старый текст");

        try (MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingNoteNewText handler = new HandleAwaitingNoteNewText();

            // When
            handler.handle(userId, chatId, " - ", bot);

            // Then
            // Проверяем, что был использован currentNoteText
            verify(noteService).updateNote(userId, "old_name", "new_name", "старый текст");
            verify(bot).sendMessage(eq(chatId), eq("Заметка успешно обновлена!\nНовое имя: \"new_name\""));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: пустой или пробельный ввод текста")
    void testHandle_EmptyInput() throws SQLException {
        // Given данные есть в FSM
        when(fsm.getTempData(userId, "oldNoteName")).thenReturn("old_name");
        when(fsm.getTempData(userId, "newNoteName")).thenReturn("new_name");
        when(fsm.getTempData(userId, "currentNoteText")).thenReturn("старый текст");

        try (MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingNoteNewText handler = new HandleAwaitingNoteNewText();

            // When
            handler.handle(userId, chatId, "   ", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Текст заметки не может быть пустым. Попробуйте снова или введите /cancel."));
            
            // Из-за раннего return база данных не трогалась
            verify(noteService, never()).updateNote(anyLong(), anyString(), anyString(), anyString());
            verify(fsm, never()).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: ошибка базы данных при обновлении заметки")
    void testHandle_DatabaseError() throws SQLException {
        // Given данные есть в FSM, но БД выбрасывает исключение
        when(fsm.getTempData(userId, "oldNoteName")).thenReturn("old_name");
        when(fsm.getTempData(userId, "newNoteName")).thenReturn("new_name");
        when(fsm.getTempData(userId, "currentNoteText")).thenReturn("старый текст");
        
        doThrow(new SQLException("DB connection failed"))
                .when(noteService).updateNote(anyLong(), anyString(), anyString(), anyString());

        try (MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingNoteNewText handler = new HandleAwaitingNoteNewText();

            // When
            handler.handle(userId, chatId, "новый текст", bot);

            // Then
            verify(noteService).updateNote(userId, "old_name", "new_name", "новый текст");
            verify(bot).sendMessage(eq(chatId), eq("Ошибка при сохранении изменений в заметке. Попробуйте позже."));
            verify(fsm).resetState(userId);
        }
    }
}