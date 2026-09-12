package bot.commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import bot.AbstractBotTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class HelpCommandTest extends AbstractBotTest {

    private Map<String, Command> commandRegistry;
    private HelpCommand helpCommand;

    @BeforeEach
    void setUp() {
        // Создаём реальный registry с командами для тестирования
        commandRegistry = Map.of(
                "help", new HelpCommand(Map.of()),
                "about", new AboutCommand(),
                "author", new AuthorCommand()
        );
        // Пересоздаём HelpCommand с полным registry
        helpCommand = new HelpCommand(commandRegistry);
    }

    @Test
    @DisplayName("Тест execute: без аргументов возвращает список всех команд")
    void testExecute_WithoutArgs_ShouldReturnAllCommands() {
        // When
        helpCommand.execute(bot, message, new String[]{});

        // Then
        verify(bot).sendMessage(eq(chatId), anyString());
    }

    @Test
    @DisplayName("Тест execute: с валидным аргументом возвращает помощь по конкретной команде")
    void testExecute_WithValidCommandArg_ShouldReturnSpecificCommandHelp() {
        // When
        helpCommand.execute(bot, message, new String[]{"about"});

        // Then
        verify(bot).sendMessage(eq(chatId), anyString());
    }

    @Test
    @DisplayName("Тест execute: с невалидным аргументом возвращает сообщение об ошибке")
    void testExecute_WithInvalidCommandArg_ShouldReturnNotFoundMessage() {
        // When
        helpCommand.execute(bot, message, new String[]{"nonexistent"});

        // Then
        verify(bot).sendMessage(eq(chatId), eq("Команда 'nonexistent' не найдена."));
    }

    @Test
    @DisplayName("Тест геттеров")
    void testGetters() {
        assertEquals("help", helpCommand.getCommandName());
        assertEquals("Показать помощь по командам", helpCommand.getDescription());
        assertEquals("/help [команда]", helpCommand.getUsage());
    }
}