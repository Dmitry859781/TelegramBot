package bot.commands;

import bot.TelegramBot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Message;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorCommandTest {

    @Mock
    private TelegramBot mockBot;

    @Mock
    private Message mockMessage;

    private AuthorCommand authorCommand;

    @BeforeEach
    void setUp() {
        authorCommand = new AuthorCommand();
    }

    @Test
    void testExecute_ShouldSendMessageWithAuthorInfo() {
        // Given
        when(mockMessage.getChatId()).thenReturn(123L);

        // When
        authorCommand.execute(mockBot, mockMessage, new String[]{});

        // Then
        verify(mockBot).sendMessage(eq(123L), anyString());
    }

    @Test
    void testGetters() {
        // Then
        assert "author".equals(authorCommand.getCommandName());
        assert "Показать информацию об авторе".equals(authorCommand.getDescription());
        assert "/author".equals(authorCommand.getUsage());
    }
}