package bot.commands;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import bot.AbstractBotTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class AuthorCommandTest extends AbstractBotTest {

    private final AuthorCommand authorCommand = new AuthorCommand();

    @Test
    @DisplayName("Тест execute: отправляет сообщение с информацией об авторе")
    void testExecute_ShouldSendMessageWithAuthorInfo() {
        // When
        authorCommand.execute(bot, message, new String[]{});

        // Then
        verify(bot).sendMessage(eq(chatId), anyString());
    }

    @Test
    @DisplayName("Тест геттеров")
    void testGetters() {
        assertEquals("author", authorCommand.getCommandName());
        assertEquals("Показать информацию об авторе", authorCommand.getDescription());
        assertEquals("/author", authorCommand.getUsage());
    }
}