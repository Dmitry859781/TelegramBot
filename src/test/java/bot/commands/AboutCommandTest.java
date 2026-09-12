package bot.commands;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import bot.AbstractBotTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class AboutCommandTest extends AbstractBotTest {

    private final AboutCommand aboutCommand = new AboutCommand();

    @Test
    @DisplayName("Тест execute: отправляет сообщение с информацией о боте")
    void testExecute_ShouldSendMessageWithInfo() {
        // When
        aboutCommand.execute(bot, message, new String[]{});

        // Then
        verify(bot).sendMessage(eq(chatId), anyString());
    }

    @Test
    @DisplayName("Тест геттеров")
    void testGetters() {
        assertEquals("about", aboutCommand.getCommandName());
        assertEquals("Показать информацию о боте", aboutCommand.getDescription());
        assertEquals("/about", aboutCommand.getUsage());
    }
}