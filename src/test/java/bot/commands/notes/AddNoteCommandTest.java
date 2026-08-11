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

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddNoteCommandTest {

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

    private AddNoteCommand addNoteCommand;

    @BeforeEach
    void setUp() {
        // Внедряем mock-объекты в конструктор команды
        addNoteCommand = new AddNoteCommand(noteService, fsm);
    }

    @Test
    @DisplayName("Тест execute: отправляет сообщение и устанавливает состояние")
    void testExecuteSendsMessageAndSetsState() {
        // Given
        Long userId = 12345L;
        Long chatId = 67890L;

        when(mockUser.getId()).thenReturn(userId);
        when(message.getFrom()).thenReturn(mockUser);
        when(message.getChatId()).thenReturn(chatId);

        // When
        addNoteCommand.execute(bot, message, new String[]{});

        // Then
        verify(bot).sendMessage(eq(chatId), eq("Введите имя добавляемой заметки."));
        verify(fsm).setState(eq(userId), eq(UserState.AWAITING_NOTE_NAME_ADD));
    }
}