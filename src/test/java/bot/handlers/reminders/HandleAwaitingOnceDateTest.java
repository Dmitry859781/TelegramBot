package bot.handlers.reminders;

import bot.AbstractBotTest;
import bot.fsm.BotFSM;
import bot.reminder.ReminderService;
import bot.timezone.UserTimezoneService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HandleAwaitingOnceDateTest extends AbstractBotTest {

    @Mock
    private UserTimezoneService timezoneService;

    @Mock
    private BotFSM fsm;

    @Mock
    private ReminderService reminderService;

    // Вспомогательный метод для настройки общих моков
    private void setupCommonMocks() throws SQLException {
        when(fsm.getTempData(userId, "reminderName")).thenReturn("test_reminder");
        when(timezoneService.getTimezone(userId)).thenReturn(3);
    }

    @Test
    @DisplayName("Тест handle: пустой или пробельный ввод")
    void testHandle_EmptyInput() throws SQLException {
        setupCommonMocks();

        try (MockedStatic<UserTimezoneService> mockedTz = Mockito.mockStatic(UserTimezoneService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedTz.when(UserTimezoneService::getInstance).thenReturn(timezoneService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingOnceDate handler = new HandleAwaitingOnceDate();

            // When
            handler.handle(userId, chatId, "   ", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Дата и время не могут быть пустыми."));
            verify(fsm).resetState(userId);
            verify(reminderService, never()).addOnceReminder(anyLong(), anyString(), any(), any());
        }
    }

    @Test
    @DisplayName("Тест handle: неверный формат даты/времени")
    void testHandle_InvalidFormat() throws SQLException {
        setupCommonMocks();

        try (MockedStatic<UserTimezoneService> mockedTz = Mockito.mockStatic(UserTimezoneService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedTz.when(UserTimezoneService::getInstance).thenReturn(timezoneService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingOnceDate handler = new HandleAwaitingOnceDate();

            // When
            handler.handle(userId, chatId, "неверная-строка", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), contains("Неверный формат даты/времени"));
            verify(fsm, never()).resetState(userId);
            verify(reminderService, never()).addOnceReminder(anyLong(), anyString(), any(), any());
        }
    }

    @Test
    @DisplayName("Тест handle: дата в прошлом (с явно указанным годом)")
    void testHandle_DateInThePast() throws SQLException {
        setupCommonMocks();

        try (MockedStatic<UserTimezoneService> mockedTz = Mockito.mockStatic(UserTimezoneService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedTz.when(UserTimezoneService::getInstance).thenReturn(timezoneService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingOnceDate handler = new HandleAwaitingOnceDate();

            // When: явно указываем прошлый год
            handler.handle(userId, chatId, "01-01-2020 10:00", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Дата и время не могут быть в прошлом. Укажите будущее время."));
            verify(fsm, never()).resetState(userId);
            verify(reminderService, never()).addOnceReminder(anyLong(), anyString(), any(), any());
        }
    }

    @Test
    @DisplayName("Тест handle: успешное добавление (с годом)")
    void testHandle_SuccessWithYear() throws SQLException {
        setupCommonMocks();

        try (MockedStatic<UserTimezoneService> mockedTz = Mockito.mockStatic(UserTimezoneService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedTz.when(UserTimezoneService::getInstance).thenReturn(timezoneService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingOnceDate handler = new HandleAwaitingOnceDate();

            // When: дата в далеком будущем
            handler.handle(userId, chatId, "31-12-2099 23:59", bot);

            // Then
            verify(reminderService).addOnceReminder(eq(userId), eq("test_reminder"), any(LocalDateTime.class), eq(ZoneOffset.ofHours(3)));
            verify(bot).sendMessage(eq(chatId), contains("Разовое напоминание \"test_reminder\" добавлено на 31.12.2099 23:59"));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: успешное добавление (без года, умный перенос на следующий год)")
    void testHandle_SuccessWithoutYearRollover() throws SQLException {
        setupCommonMocks();

        try (MockedStatic<UserTimezoneService> mockedTz = Mockito.mockStatic(UserTimezoneService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedTz.when(UserTimezoneService::getInstance).thenReturn(timezoneService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingOnceDate handler = new HandleAwaitingOnceDate();

            // When: 1 января без года
            handler.handle(userId, chatId, "01-01 10:00", bot);

            // Then
            verify(reminderService).addOnceReminder(eq(userId), eq("test_reminder"), any(LocalDateTime.class), eq(ZoneOffset.ofHours(3)));
            verify(bot).sendMessage(eq(chatId), contains("Разовое напоминание \"test_reminder\" добавлено на"));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: ошибка БД - имя напоминания уже существует")
    void testHandle_DbErrorNameExists() throws SQLException {
        setupCommonMocks();
        
        // Имитируем ошибку, которую выбрасывает ReminderService при нарушении UNIQUE constraint
        doThrow(new RuntimeException("Напоминание с именем 'test_reminder' уже существует"))
                .when(reminderService).addOnceReminder(anyLong(), anyString(), any(), any());

        try (MockedStatic<UserTimezoneService> mockedTz = Mockito.mockStatic(UserTimezoneService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedTz.when(UserTimezoneService::getInstance).thenReturn(timezoneService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingOnceDate handler = new HandleAwaitingOnceDate();

            // When
            handler.handle(userId, chatId, "31-12-2099 23:59", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), contains("Напоминание с именем \"test_reminder\" уже существует"));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: общая ошибка БД при добавлении")
    void testHandle_GeneralDbError() throws SQLException {
        setupCommonMocks();
        
        // ИЗМЕНЕНО: используем RuntimeException, так как ReminderService 
        // оборачивает SQLException именно в него (см. метод addReminder)
        doThrow(new RuntimeException("Database connection lost"))
                .when(reminderService).addOnceReminder(anyLong(), anyString(), any(), any());

        try (MockedStatic<UserTimezoneService> mockedTz = Mockito.mockStatic(UserTimezoneService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedTz.when(UserTimezoneService::getInstance).thenReturn(timezoneService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingOnceDate handler = new HandleAwaitingOnceDate();

            // When
            handler.handle(userId, chatId, "31-12-2099 23:59", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Ошибка при добавлении напоминания. Попробуйте позже."));
            verify(fsm).resetState(userId);
        }
    }
}