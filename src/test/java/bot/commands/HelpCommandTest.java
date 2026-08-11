package bot.commands;

import bot.TelegramBot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HelpCommandTest {

    @Mock
    private TelegramBot mockBot;

    @Mock
    private Message mockMessage;

    private Map<String, Command> commandRegistry;
    private HelpCommand helpCommand;

    @BeforeEach
    void setUp() {
        // Создаём реальный registry с командами для тестирования
        commandRegistry = Map.of(
                "help", new HelpCommand(Map.of()), // важно: передаём пустой, т.к. HelpCommand сам добавляется
                "about", new AboutCommand(),
                "author", new AuthorCommand()
        );
        // Пересоздаём HelpCommand с полным registry
        helpCommand = new HelpCommand(commandRegistry);
    }

    @Test
    void testExecute_WithoutArgs_ShouldReturnAllCommands() {
        // Given
        when(mockMessage.getChatId()).thenReturn(123L);

        // When
        helpCommand.execute(mockBot, mockMessage, new String[]{});

        // Then
        verify(mockBot).sendMessage(eq(123L), anyString());
    }

    @Test
    void testExecute_WithValidCommandArg_ShouldReturnSpecificCommandHelp() {
        // Given
        when(mockMessage.getChatId()).thenReturn(123L);

        // When
        helpCommand.execute(mockBot, mockMessage, new String[]{"about"});

        // Then
        verify(mockBot).sendMessage(anyLong(), anyString());
    }

    @Test
    void testExecute_WithInvalidCommandArg_ShouldReturnNotFoundMessage() {
        // Given
        when(mockMessage.getChatId()).thenReturn(123L);

        // When
        helpCommand.execute(mockBot, mockMessage, new String[]{"nonexistent"});

        // Then
        verify(mockBot).sendMessage(anyLong(), eq("Команда 'nonexistent' не найдена."));
    }

    @Test
    void testGetters() {
        // Then
        assert "help".equals(helpCommand.getCommandName());
        assert "Показать помощь по командам".equals(helpCommand.getDescription());
        assert "/help [команда]".equals(helpCommand.getUsage());
    }
}