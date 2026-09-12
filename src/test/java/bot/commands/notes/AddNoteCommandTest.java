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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

class AddNoteCommandTest extends AbstractBotTest {

    @Mock
    private NoteService noteService;

    @Test
    @DisplayName("Тест execute: отправляет сообщение и устанавливает состояние")
    void testExecuteSendsMessageAndSetsState() {
        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<NoteService> mockedNoteService = Mockito.mockStatic(NoteService.class)) {

            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedNoteService.when(NoteService::getInstance).thenReturn(noteService);

            AddNoteCommand command = new AddNoteCommand();

            // When
            command.execute(bot, message, new String[]{});

            // Then
            verify(bot).sendMessage(chatId, "Введите имя добавляемой заметки.");
            verify(fsm).setState(userId, UserState.AWAITING_NOTE_NAME_TO_ADD);
        }
    }

    @Test
    @DisplayName("Тест геттеров")
    void testGetters() {
        AddNoteCommand command = new AddNoteCommand();

        assertEquals("addNote", command.getCommandName());
        assertEquals("Добавить заметку", command.getDescription());
        assertEquals("/addNote", command.getUsage());
    }
}