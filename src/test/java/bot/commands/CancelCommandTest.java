package bot.commands;

import bot.AbstractBotTest;
import bot.fsm.BotFSM;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class CancelCommandTest extends AbstractBotTest {

    @Mock
    private BotFSM fsm;

    private CancelCommand cancelCommand;

    @BeforeEach
    void setUp() {
        cancelCommand = new CancelCommand(fsm);
    }

    @Test
    @DisplayName("Тест execute: сбрасывает состояние и отправляет сообщение об отмене")
    void testExecuteCancelsOperation() {
        // When
        cancelCommand.execute(bot, message, new String[]{});

        // Then
        verify(fsm).resetState(userId);
        verify(bot).sendMessage(eq(chatId), eq("Операция отменена. Вы возвращены в главное меню."));
    }

    @Test
    @DisplayName("Тест геттеров")
    void testGetters() {
        assertEquals("cancel", cancelCommand.getCommandName());
        assertEquals("Отменить текущую операцию и вернуться в главное меню", cancelCommand.getDescription());
        assertEquals("/cancel", cancelCommand.getUsage());
    }
}