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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HandleAwaitingReminderNewTimeTest extends AbstractBotTest {

    @Mock
    private BotFSM fsm;

    @Mock
    private ReminderService reminderService;

    @Mock
    private UserTimezoneService timezoneService;

    // Вспомогательный метод для настройки данных из FSM
    private void setupFsmMocks(String oldName, String newName, String oldProps, String type) throws SQLException {
        when(fsm.getTempData(userId, "oldReminderName")).thenReturn(oldName);
        when(fsm.getTempData(userId, "newReminderName")).thenReturn(newName);
        when(fsm.getTempData(userId, "oldPropertiesJson")).thenReturn(oldProps);
        when(fsm.getTempData(userId, "reminderType")).thenReturn(type);
        when(timezoneService.getTimezone(userId)).thenReturn(3);
    }

    // Вспомогательный метод для настройки моков синглтонов
    private AutoCloseable setupStaticMocks() {
        MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
        MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class);
        MockedStatic<UserTimezoneService> mockedTz = Mockito.mockStatic(UserTimezoneService.class);

        mockedFsm.when(BotFSM::getInstance).thenReturn(this.fsm);
        mockedRs.when(ReminderService::getInstance).thenReturn(this.reminderService);
        mockedTz.when(UserTimezoneService::getInstance).thenReturn(this.timezoneService);

        // Возвращаем объект, который закроет все 3 мока одновременно
        return () -> {
            mockedFsm.close();
            mockedRs.close();
            mockedTz.close();
        };
    }

    @Test
    @DisplayName("Тест handle: пустой или пробельный ввод")
    void testHandle_EmptyInput() throws Exception {
        setupFsmMocks("old", "new", "{}", "ONCE");

        try (AutoCloseable mocks = setupStaticMocks()) {
            HandleAwaitingReminderNewTime handler = new HandleAwaitingReminderNewTime();

            // When
            handler.handle(userId, chatId, "   ", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Ввод не может быть пустым. Попробуйте снова или введите /cancel."));
            verify(reminderService, never()).updateReminder(anyLong(), anyString(), anyString(), anyString());
            verify(fsm, never()).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: ввод '-' (оставить старые свойства) - Успех")
    void testHandle_KeepOldProperties_Success() throws Exception {
        setupFsmMocks("old_name", "new_name", "{\"remind_at\":\"2024-01-01 10:00:00\"}", "ONCE");

        try (AutoCloseable mocks = setupStaticMocks()) {
            HandleAwaitingReminderNewTime handler = new HandleAwaitingReminderNewTime();

            // When
            handler.handle(userId, chatId, " - ", bot);

            // Then
            verify(reminderService).updateReminder(userId, "old_name", "new_name", "{\"remind_at\":\"2024-01-01 10:00:00\"}");
            verify(bot).sendMessage(eq(chatId), eq("Напоминание \"new_name\" успешно обновлено!"));
            
            // Проверка блока finally
            verify(fsm).clearTempData(userId, "oldReminderName");
            verify(fsm).clearTempData(userId, "newReminderName");
            verify(fsm).clearTempData(userId, "oldPropertiesJson");
            verify(fsm).clearTempData(userId, "reminderType");
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: ввод '-' - Ошибка UNIQUE constraint")
    void testHandle_KeepOldProperties_UniqueConstraintError() throws Exception {
        setupFsmMocks("old_name", "new_name", "{}", "ONCE");
        doThrow(new SQLException("UNIQUE constraint failed: reminders.user_id, reminders.reminder_name"))
                .when(reminderService).updateReminder(anyLong(), anyString(), anyString(), anyString());

        try (AutoCloseable mocks = setupStaticMocks()) {
            HandleAwaitingReminderNewTime handler = new HandleAwaitingReminderNewTime();

            // When
            handler.handle(userId, chatId, "-", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Напоминание с именем \"new_name\" уже существует. Операция отменена."));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: ONCE - валидная дата с годом")
    void testHandle_Once_ValidDateWithYear() throws Exception {
        setupFsmMocks("old", "new", "{}", "ONCE");
        when(timezoneService.getTimezone(userId)).thenReturn(3);

        try (AutoCloseable mocks = setupStaticMocks()) {
            HandleAwaitingReminderNewTime handler = new HandleAwaitingReminderNewTime();

            // When
            handler.handle(userId, chatId, "31-12-2099 23:59", bot);

            // Then
            // Проверяем, что дата сконвертирована в UTC и отформатирована правильно
            verify(reminderService).updateReminder(eq(userId), eq("old"), eq("new"), contains("2099-12-31 20:59:00"));
            verify(bot).sendMessage(eq(chatId), eq("Напоминание \"new\" успешно обновлено!"));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: ONCE - дата без года (умный перенос на следующий год)")
    void testHandle_Once_DateWithoutYearRollover() throws Exception {
        setupFsmMocks("old", "new", "{}", "ONCE");
        when(timezoneService.getTimezone(userId)).thenReturn(3);

        try (AutoCloseable mocks = setupStaticMocks()) {
            HandleAwaitingReminderNewTime handler = new HandleAwaitingReminderNewTime();

            // WhenЕсли тест запускается не раньше 1 января в 10:00, то это будет в прошлом и код должен добавить 1 год.
            handler.handle(userId, chatId, "01-01 10:00", bot);

            // Then
            verify(reminderService).updateReminder(eq(userId), eq("old"), eq("new"), anyString());
            verify(bot).sendMessage(eq(chatId), eq("Напоминание \"new\" успешно обновлено!"));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: ONCE - дата в прошлом (даже с переносом года)")
    void testHandle_Once_DateInThePast() throws Exception {
        setupFsmMocks("old", "new", "{}", "ONCE");
        when(timezoneService.getTimezone(userId)).thenReturn(3);

        try (AutoCloseable mocks = setupStaticMocks()) {
            HandleAwaitingReminderNewTime handler = new HandleAwaitingReminderNewTime();

            // When: явно указываем прошлый год
            handler.handle(userId, chatId, "01-01-2020 10:00", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Неверный формат. Попробуйте снова или введите /cancel."));
            verify(reminderService, never()).updateReminder(anyLong(), anyString(), anyString(), anyString());
            verify(fsm, never()).resetState(userId); // Ранний return
        }
    }

    @Test
    @DisplayName("Тест handle: ONCE - неверный формат даты")
    void testHandle_Once_InvalidFormat() throws Exception {
        setupFsmMocks("old", "new", "{}", "ONCE");

        try (AutoCloseable mocks = setupStaticMocks()) {
            HandleAwaitingReminderNewTime handler = new HandleAwaitingReminderNewTime();

            // When
            handler.handle(userId, chatId, "неверная-строка", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Неверный формат. Попробуйте снова или введите /cancel."));
            verify(reminderService, never()).updateReminder(anyLong(), anyString(), anyString(), anyString());
            verify(fsm, never()).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: RECURRING - валидный ввод (ПН-ПТ 09:00)")
    void testHandle_Recurring_ValidInput() throws Exception {
        setupFsmMocks("old", "new", "{}", "RECURRING");

        try (AutoCloseable mocks = setupStaticMocks()) {
            HandleAwaitingReminderNewTime handler = new HandleAwaitingReminderNewTime();

            // When
            handler.handle(userId, chatId, "ПН-ПТ 09:00", bot);

            // Then
            // Проверяем, что JSON содержит дни MON-FRI и время 09:00
            verify(reminderService).updateReminder(eq(userId), eq("old"), eq("new"), argThat(json -> 
                json.contains("\"day\":\"MON\"") && 
                json.contains("\"day\":\"FRI\"") && 
                json.contains("\"time\":\"09:00\"")
            ));
            verify(bot).sendMessage(eq(chatId), eq("Напоминание \"new\" успешно обновлено!"));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: RECURRING - неверный формат времени")
    void testHandle_Recurring_InvalidTime() throws Exception {
        setupFsmMocks("old", "new", "{}", "RECURRING");

        try (AutoCloseable mocks = setupStaticMocks()) {
            HandleAwaitingReminderNewTime handler = new HandleAwaitingReminderNewTime();

            // When
            handler.handle(userId, chatId, "ПН 25:00", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Неверный формат. Попробуйте снова или введите /cancel."));
            verify(reminderService, never()).updateReminder(anyLong(), anyString(), anyString(), anyString());
            verify(fsm, never()).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: RECURRING - неверный формат дней")
    void testHandle_Recurring_InvalidDays() throws Exception {
        setupFsmMocks("old", "new", "{}", "RECURRING");

        try (AutoCloseable mocks = setupStaticMocks()) {
            HandleAwaitingReminderNewTime handler = new HandleAwaitingReminderNewTime();

            // When
            handler.handle(userId, chatId, "XYZ 09:00", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Неверный формат. Попробуйте снова или введите /cancel."));
            verify(reminderService, never()).updateReminder(anyLong(), anyString(), anyString(), anyString());
            verify(fsm, never()).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: общая ошибка БД при сохранении")
    void testHandle_GenericDatabaseError() throws Exception {
        setupFsmMocks("old", "new", "{}", "RECURRING");
        doThrow(new SQLException("Database connection lost"))
                .when(reminderService).updateReminder(anyLong(), anyString(), anyString(), anyString());

        try (AutoCloseable mocks = setupStaticMocks()) {
            HandleAwaitingReminderNewTime handler = new HandleAwaitingReminderNewTime();

            // When
            handler.handle(userId, chatId, "ПН 09:00", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Ошибка при сохранении изменений в базе данных."));
            verify(fsm).resetState(userId); // finally должен сработать
        }
    }
}