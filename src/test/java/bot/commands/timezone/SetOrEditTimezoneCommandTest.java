package bot.commands.timezone;

import bot.AbstractBotTest;
import bot.fsm.BotFSM;
import bot.fsm.UserState;
import bot.timezone.UserTimezoneService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SetOrEditTimezoneCommandTest extends AbstractBotTest {

    @Mock
    private UserTimezoneService timezoneService;

    @Mock
    private BotFSM fsm;

    @Test
    @DisplayName("Тест execute: у пользователя нет установленного часового пояса")
    void testExecute_NoExistingTimezone() throws SQLException {
        when(timezoneService.getTimezone(userId)).thenReturn(null);

        try (MockedStatic<UserTimezoneService> mockedTz = Mockito.mockStatic(UserTimezoneService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedTz.when(UserTimezoneService::getInstance).thenReturn(timezoneService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            SetOrEditTimezoneCommand command = new SetOrEditTimezoneCommand();
            command.execute(bot, message, new String[]{});

            String expectedText = "Пожалуйста, укажите ваш часовой пояс как смещение от UTC.\n\n" +
                    "Примеры:\n" +
                    "Москва → +3\n" +
                    "Екатеринбург → +5\n" +
                    "Владивосток → +10\n" +
                    "Лондон → +0\n" +
                    "Нью-Йорк → -5\n\n" +
                    "Просто отправьте число со знаком: +3, -4, +0 и т.д.";

            verify(bot).sendMessage(eq(chatId), eq(expectedText));
            verify(fsm).setState(userId, UserState.AWAITING_TIMEZONE_OFFSET);
            verify(fsm).setTempData(userId, "command", "setOrEditTimezone");
        }
    }

    @Test
    @DisplayName("Тест execute: у пользователя установлен положительный часовой пояс")
    void testExecute_WithPositiveTimezone() throws SQLException {
        when(timezoneService.getTimezone(userId)).thenReturn(3);

        try (MockedStatic<UserTimezoneService> mockedTz = Mockito.mockStatic(UserTimezoneService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedTz.when(UserTimezoneService::getInstance).thenReturn(timezoneService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            SetOrEditTimezoneCommand command = new SetOrEditTimezoneCommand();
            command.execute(bot, message, new String[]{});

            String expectedText = "Ваш текущий часовой пояс: UTC+3\n\n" +
                    "Пожалуйста, укажите ваш часовой пояс как смещение от UTC.\n\n" +
                    "Примеры:\n" +
                    "Москва → +3\n" +
                    "Екатеринбург → +5\n" +
                    "Владивосток → +10\n" +
                    "Лондон → +0\n" +
                    "Нью-Йорк → -5\n\n" +
                    "Просто отправьте число со знаком: +3, -4, +0 и т.д.";

            verify(bot).sendMessage(eq(chatId), eq(expectedText));
            verify(fsm).setState(userId, UserState.AWAITING_TIMEZONE_OFFSET);
            verify(fsm).setTempData(userId, "command", "setOrEditTimezone");
        }
    }

    @Test
    @DisplayName("Тест execute: у пользователя установлен отрицательный часовой пояс")
    void testExecute_WithNegativeTimezone() throws SQLException {
        when(timezoneService.getTimezone(userId)).thenReturn(-5);

        try (MockedStatic<UserTimezoneService> mockedTz = Mockito.mockStatic(UserTimezoneService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedTz.when(UserTimezoneService::getInstance).thenReturn(timezoneService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            SetOrEditTimezoneCommand command = new SetOrEditTimezoneCommand();
            command.execute(bot, message, new String[]{});

            // Проверяем, что знак минус подставился корректно (без лишнего плюса)
            String expectedText = "Ваш текущий часовой пояс: UTC-5\n\n" +
                    "Пожалуйста, укажите ваш часовой пояс как смещение от UTC.\n\n" +
                    "Примеры:\n" +
                    "Москва → +3\n" +
                    "Екатеринбург → +5\n" +
                    "Владивосток → +10\n" +
                    "Лондон → +0\n" +
                    "Нью-Йорк → -5\n\n" +
                    "Просто отправьте число со знаком: +3, -4, +0 и т.д.";

            verify(bot).sendMessage(eq(chatId), eq(expectedText));
            verify(fsm).setState(userId, UserState.AWAITING_TIMEZONE_OFFSET);
            verify(fsm).setTempData(userId, "command", "setOrEditTimezone");
        }
    }

    @Test
    @DisplayName("Тест execute: ошибка базы данных при получении часового пояса")
    void testExecute_DatabaseError() throws SQLException {
        when(timezoneService.getTimezone(userId)).thenThrow(new SQLException("DB error"));

        try (MockedStatic<UserTimezoneService> mockedTz = Mockito.mockStatic(UserTimezoneService.class)) {
            mockedTz.when(UserTimezoneService::getInstance).thenReturn(timezoneService);

            SetOrEditTimezoneCommand command = new SetOrEditTimezoneCommand();
            command.execute(bot, message, new String[]{});

            verify(bot).sendMessage(eq(chatId), eq("Ошибка при загрузке часового пояса. Попробуйте позже."));
            // При ошибке состояние FSM не должно быть установлено
        }
    }

    @Test
    @DisplayName("Тест геттеров")
    void testGetters() {
        SetOrEditTimezoneCommand command = new SetOrEditTimezoneCommand();
        
        assertEquals("setOrEditTimezone", command.getCommandName());
        assertEquals("Установить или изменить ваш часовой пояс", command.getDescription());
        assertEquals("/setOrEditTimezone", command.getUsage());
    }
}