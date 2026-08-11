package bot.commands;

import bot.TelegramBot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartCommandTest {

    @Mock
    private TelegramBot mockBot;

    @Mock
    private Message mockMessage;

    private Map<String, Command> commandRegistry;
    private StartCommand startCommand;

    @BeforeEach
    void setUp() {
        // Создаём реальный registry с командами для тестирования
        commandRegistry = Map.of(
                "start", new StartCommand(Map.of()),
                "help", new HelpCommand(Map.of()),
                "about", new AboutCommand(),
                "author", new AuthorCommand()
        );
        // Пересоздаём StartCommand с полным registry
        startCommand = new StartCommand(commandRegistry);
    }

    @Test
    @DisplayName("Тест execute: отправляет список команд")
    void testExecuteSendsListOfCommands() {
        // Given
        when(mockMessage.getChatId()).thenReturn(123L);

        // When
        startCommand.execute(mockBot, mockMessage, new String[]{});

        // Then
        verify(mockBot).sendMessage(eq(123L), anyString());
    }

    @Test
    @DisplayName("Тест execute: отправляет корректный текст списка команд")
    void testExecuteSendsCorrectCommandList() {
        // Given
        when(mockMessage.getChatId()).thenReturn(123L);

        // When
        startCommand.execute(mockBot, mockMessage, new String[]{});

        // Then
        verify(mockBot).sendMessage(eq(123L), eq("Доступные команды:\n<code>/start</code> - Показать список команд\n<code>/help</code> - Показать помощь по командам\n<code>/about</code> - Информация о боте\n<code>/author</code> - Информация об авторе\n"));
    }

    @Test
    @DisplayName("Тест execute: не использует аргументы")
    void testExecuteIgnoresArguments() {
        // Given
        when(mockMessage.getChatId()).thenReturn(123L);

        // When
        startCommand.execute(mockBot, mockMessage, new String[]{"some", "arguments"});

        // Then
        verify(mockBot).sendMessage(eq(123L), eq("Доступные команды:\n<code>/start</code> - Показать список команд\n<code>/help</code> - Показать помощь по командам\n<code>/about</code> - Информация о боте\n<code>/author</code> - Информация об авторе\n"));
    }

    @Test
    @DisplayName("Тест геттеров")
    void testGetters() {
        // Then
        assert "start".equals(startCommand.getCommandName());
        assert "Показать список команд".equals(startCommand.getDescription());
        assert "/start".equals(startCommand.getUsage());
    }
}