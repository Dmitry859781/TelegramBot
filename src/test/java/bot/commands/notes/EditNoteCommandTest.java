package bot.commands.notes;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class EditNoteCommandTest extends AbstractBotTest {

    @Mock
    private NoteService noteService;

    @Test
    @DisplayName("Тест execute: выводит список заметок и устанавливает состояние")
    void testExecuteSendsNoteListAndSetsState() throws SQLException {
        List<String> noteNames = List.of("note1", "note2");
        when(noteService.getUserNotes(userId)).thenReturn(noteNames);

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class)) {

            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);

            EditNoteCommand command = new EditNoteCommand();

            // When
            command.execute(bot, message, new String[]{});

            // Then
            verify(bot).sendMessage(chatId, "Какую заметку хотите отредактировать?\n1. note1\n2. note2");
            verify(bot).sendMessage(chatId, "Введите имя редактируемой заметки.");
            verify(fsm).setState(userId, UserState.AWAITING_NOTE_NAME_TO_EDIT);
        }
    }

    @Test
    @DisplayName("Тест execute: выводит сообщение, если нет заметок")
    void testExecuteSendsMessageIfNoNotes() throws SQLException {
        when(noteService.getUserNotes(userId)).thenReturn(List.of());

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class)) {

            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);

            EditNoteCommand command = new EditNoteCommand();

            // When
            command.execute(bot, message, new String[]{});

            // Then
            verify(bot).sendMessage(chatId, "У вас пока нет заметок. Добавьте первую с помощью /addNote");
            verify(fsm, never()).setState(anyLong(), any());
        }
    }

    @Test
    @DisplayName("Тест execute: обрабатывает ошибку получения списка заметок")
    void testExecuteHandlesException() throws SQLException {
        when(noteService.getUserNotes(userId)).thenThrow(new RuntimeException("Database error"));

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class)) {

            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);

            EditNoteCommand command = new EditNoteCommand();

            // When
            command.execute(bot, message, new String[]{});

            // Then
            verify(bot).sendMessage(chatId, "Не удалось вывести список заметок. Попробуйте позже.");
            verify(fsm, never()).setState(anyLong(), any());
        }
    }

    @Test
    @DisplayName("Тест геттеров")
    void testGetters() {
        EditNoteCommand command = new EditNoteCommand();

        assertEquals("editNote", command.getCommandName());
        assertEquals("Редактировать заметку", command.getDescription());
        assertEquals("/editNote", command.getUsage());
    }
}