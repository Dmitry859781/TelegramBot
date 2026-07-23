package bot.commands;

import bot.TelegramBot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Message;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class AboutCommandTest {

    @Mock
    private TelegramBot mockBot;

    @Mock
    private Message mockMessage;

    private AboutCommand aboutCommand;

    @BeforeEach
    void setUp() {
        aboutCommand = new AboutCommand();
    }

    @Test
    void testExecute_ShouldSendMessageWithInfo() {
        // Given
        when(mockMessage.getChatId()).thenReturn(123L);

        // When
        aboutCommand.execute(mockBot, mockMessage, new String[]{});

        // Then
        verify(mockBot).sendMessage(eq(123L), anyString());
    }

    @Test
    void testGetters() {
        // Then
        assert "about".equals(aboutCommand.getCommandName());
        assert "Показать информацию о боте".equals(aboutCommand.getDescription());
        assert "/about".equals(aboutCommand.getUsage());
    }
}