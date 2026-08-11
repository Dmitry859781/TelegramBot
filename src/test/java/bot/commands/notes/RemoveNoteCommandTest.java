package bot.commands.notes;

import bot.TelegramBot;
import bot.fsm.BotFSM;
import bot.fsm.UserState;
import bot.note.NoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.sql.SQLException;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemoveNoteCommandTest {

    @Mock
    private NoteService noteService;

    @Mock
    private BotFSM fsm;

    @Mock
    private TelegramBot bot;

    @Mock
    private Message message;

    @Mock
    private User mockUser;

    private RemoveNoteCommand removeNoteCommand;

    @BeforeEach
    void setUp() {
        removeNoteCommand = new RemoveNoteCommand(noteService, fsm);
    }

    @Test
    @DisplayName("Тест execute: выводит список заметок и устанавливает состояние")
    void testExecuteSendsNoteListAndSetsState() throws SQLException {
        // Given
        Long userId = 12345L;
        Long chatId = 67890L;
        List<String> noteNames = List.of("note1", "note2");

        when(mockUser.getId()).thenReturn(userId);
        when(message.getFrom()).thenReturn(mockUser);
        when(message.getChatId()).thenReturn(chatId);
        when(noteService.getUserNotes(userId)).thenReturn(noteNames);

        // When
        removeNoteCommand.execute(bot, message, new String[]{});

        // Then
        verify(bot).sendMessage(eq(chatId), eq("Какую заметку хотите удалить?\n1. note1\n2. note2"));
        verify(bot).sendMessage(eq(chatId), eq("Введите имя заметки, которую хотите удалить."));
        verify(fsm).setState(eq(userId), eq(UserState.AWAITING_NOTE_NAME_REMOVE));
    }

    @Test
    @DisplayName("Тест execute: выводит сообщение, если нет заметок")
    void testExecuteSendsMessageIfNoNotes() throws SQLException {
        // Given
        Long userId = 12345L;
        Long chatId = 67890L;

        when(mockUser.getId()).thenReturn(userId);
        when(message.getFrom()).thenReturn(mockUser);
        when(message.getChatId()).thenReturn(chatId);
        when(noteService.getUserNotes(userId)).thenReturn(List.of());

        // When
        removeNoteCommand.execute(bot, message, new String[]{});

        // Then
        verify(bot).sendMessage(eq(chatId), eq("У вас пока нет заметок. Добавьте первую с помощью /addNote"));
        verify(fsm, never()).setState(any(), any()); // Состояние не должно быть установлено
    }

    @Test
    @DisplayName("Тест execute: обрабатывает ошибку получения списка заметок")
    void testExecuteHandlesException() throws SQLException {
        // Given
        Long userId = 12345L;
        Long chatId = 67890L;

        when(mockUser.getId()).thenReturn(userId);
        when(message.getFrom()).thenReturn(mockUser);
        when(message.getChatId()).thenReturn(chatId);
        when(noteService.getUserNotes(userId)).thenThrow(new RuntimeException("Database error"));

        // When
        removeNoteCommand.execute(bot, message, new String[]{});

        // Then
        verify(bot).sendMessage(eq(chatId), eq("Не удалось вывести список заметок. Попробуйте позже."));
        verify(fsm, never()).setState(any(), any()); // Состояние не должно быть установлено
    }
}