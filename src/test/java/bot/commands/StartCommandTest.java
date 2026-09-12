package bot.commands;

import bot.AbstractBotTest;
import bot.timezone.UserTimezoneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StartCommandTest extends AbstractBotTest {

    private Map<String, Command> commandRegistry;
    private StartCommand startCommand;

    @Mock
    private UserTimezoneService timezoneService;

    @BeforeEach
    void setUp() {
        commandRegistry = new LinkedHashMap<>();
        commandRegistry.put("start", new StartCommand(Map.of()));
        commandRegistry.put("help", new HelpCommand(Map.of()));
        commandRegistry.put("about", new AboutCommand());
        commandRegistry.put("author", new AuthorCommand());
    }

    @Test
    @DisplayName("Тест execute: часовая зона не установлена")
    void testExecute_TimezoneNotSet() throws SQLException {
        when(timezoneService.getTimezone(userId)).thenReturn(null);

        try (MockedStatic<UserTimezoneService> mockedTz = Mockito.mockStatic(UserTimezoneService.class)) {
            mockedTz.when(UserTimezoneService::getInstance).thenReturn(timezoneService);
            startCommand = new StartCommand(commandRegistry);
            
            // When
            startCommand.execute(bot, message, new String[]{});

            // Then
            verify(bot).sendMessage(eq(chatId), eq("У вас не установлена временная зона, используйте команду /setOrEditTimezone для установки."));
        }
    }

    @Test
    @DisplayName("Тест execute: часовая зона установлена, отправляет список команд")
    void testExecute_TimezoneSet_SendsCommandList() throws SQLException {
        when(timezoneService.getTimezone(userId)).thenReturn(3);

        try (MockedStatic<UserTimezoneService> mockedTz = Mockito.mockStatic(UserTimezoneService.class)) {
            mockedTz.when(UserTimezoneService::getInstance).thenReturn(timezoneService);
            startCommand = new StartCommand(commandRegistry);

            // When
            startCommand.execute(bot, message, new String[]{});

            // Then
            String expectedText = "Доступные команды:\n"
                    + "/start — Показать список команд\n"
                    + "/help — Показать помощь по командам\n"
                    + "/about — Показать информацию о боте\n"
                    + "/author — Показать информацию об авторе\n";
            
            verify(bot).sendMessage(eq(chatId), eq(expectedText));
        }
    }

    @Test
    @DisplayName("Тест execute: ошибка базы данных при проверке зоны")
    void testExecute_DatabaseError() throws SQLException {
        when(timezoneService.getTimezone(userId)).thenThrow(new SQLException("DB error"));

        try (MockedStatic<UserTimezoneService> mockedTz = Mockito.mockStatic(UserTimezoneService.class)) {
            mockedTz.when(UserTimezoneService::getInstance).thenReturn(timezoneService);
            startCommand = new StartCommand(commandRegistry);

            // When
            startCommand.execute(bot, message, new String[]{});

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Ошибка при проверке настроек. Попробуйте позже."));
        }
    }

    @Test
    @DisplayName("Тест геттеров")
    void testGetters() {
    	startCommand = new StartCommand(commandRegistry);
    	
        assertEquals("start", startCommand.getCommandName());
        assertEquals("Показать список команд", startCommand.getDescription());
        assertEquals("/start", startCommand.getUsage());
    }
}