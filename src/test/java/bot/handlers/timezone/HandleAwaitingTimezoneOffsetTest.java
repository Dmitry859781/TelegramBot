package bot.handlers.timezone;

import bot.AbstractBotTest;
import bot.fsm.BotFSM;
import bot.timezone.UserTimezoneService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.SQLException;

class HandleAwaitingTimezoneOffsetTest extends AbstractBotTest {

    @Mock
    private UserTimezoneService timezoneService;

    @Mock
    private BotFSM fsm;

    @Test
    @DisplayName("Тест handle: успешная установка положительного смещения (+3)")
    void testHandle_SuccessPositiveOffset() throws SQLException {
        try (MockedStatic<UserTimezoneService> mockedTz = Mockito.mockStatic(UserTimezoneService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedTz.when(UserTimezoneService::getInstance).thenReturn(timezoneService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingTimezoneOffset handler = new HandleAwaitingTimezoneOffset();

            // When
            handler.handle(userId, chatId, "+3", bot);

            // Then
            verify(timezoneService).saveTimezoneOffset(userId, 3);
            verify(bot).sendMessage(eq(chatId), eq("Часовой пояс установлен: UTC+3"));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: успешная установка отрицательного смещения (-5)")
    void testHandle_SuccessNegativeOffset() throws SQLException {
        try (MockedStatic<UserTimezoneService> mockedTz = Mockito.mockStatic(UserTimezoneService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedTz.when(UserTimezoneService::getInstance).thenReturn(timezoneService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingTimezoneOffset handler = new HandleAwaitingTimezoneOffset();

            // When
            handler.handle(userId, chatId, "-5", bot);

            // Then
            verify(timezoneService).saveTimezoneOffset(userId, -5);
            verify(bot).sendMessage(eq(chatId), eq("Часовой пояс установлен: UTC-5"));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: успешная установка нулевого смещения (0)")
    void testHandle_SuccessZeroOffset() throws SQLException {
        try (MockedStatic<UserTimezoneService> mockedTz = Mockito.mockStatic(UserTimezoneService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedTz.when(UserTimezoneService::getInstance).thenReturn(timezoneService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingTimezoneOffset handler = new HandleAwaitingTimezoneOffset();

            // When
            handler.handle(userId, chatId, "0", bot);

            // Then
            verify(timezoneService).saveTimezoneOffset(userId, 0);
            verify(bot).sendMessage(eq(chatId), eq("Часовой пояс установлен: UTC+0"));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: пустой или пробельный ввод")
    void testHandle_EmptyInput() throws SQLException {
        try (MockedStatic<UserTimezoneService> mockedTz = Mockito.mockStatic(UserTimezoneService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedTz.when(UserTimezoneService::getInstance).thenReturn(timezoneService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingTimezoneOffset handler = new HandleAwaitingTimezoneOffset();

            // When
            handler.handle(userId, chatId, "   ", bot);

            // Then
            verify(timezoneService, never()).saveTimezoneOffset(anyLong(), anyInt());
            verify(bot).sendMessage(eq(chatId), eq("Смещение не может быть пустым. Попробуйте снова: /setOrEditTimezone"));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: смещение вне допустимого диапазона (15)")
    void testHandle_OutOfRange() throws SQLException {
        try (MockedStatic<UserTimezoneService> mockedTz = Mockito.mockStatic(UserTimezoneService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedTz.when(UserTimezoneService::getInstance).thenReturn(timezoneService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingTimezoneOffset handler = new HandleAwaitingTimezoneOffset();

            // When
            handler.handle(userId, chatId, "15", bot);

            // Then
            verify(timezoneService, never()).saveTimezoneOffset(anyLong(), anyInt());
            verify(bot).sendMessage(eq(chatId), eq("Допустимый диапазон: от -12 до +14. Попробуйте снова: /setOrEditTimezone"));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: неверный формат ввода")
    void testHandle_InvalidFormat() throws SQLException {
        try (MockedStatic<UserTimezoneService> mockedTz = Mockito.mockStatic(UserTimezoneService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedTz.when(UserTimezoneService::getInstance).thenReturn(timezoneService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingTimezoneOffset handler = new HandleAwaitingTimezoneOffset();

            // When
            handler.handle(userId, chatId, "abc", bot);

            // Then
            verify(timezoneService, never()).saveTimezoneOffset(anyLong(), anyInt());
            
            String expectedErrorMsg = "Неверный формат.\n" +
                    "Пожалуйста, введите смещение от UTC в формате:\n" +
                    "+3, -5, +0 и т.д.\n" +
                    "Попробуйте снова";
            verify(bot).sendMessage(eq(chatId), eq(expectedErrorMsg));
            verify(fsm).resetState(userId);
        }
    }
}