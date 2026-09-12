package bot.handlers.reminders;

import bot.AbstractBotTest;
import bot.fsm.BotFSM;
import bot.reminder.ReminderService;
import bot.reminder.recurring.RecurringProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HandleAwaitingRecurringDaysTest extends AbstractBotTest {

    @Mock
    private BotFSM fsm;

    @Mock
    private ReminderService reminderService;

    // Вспомогательный метод для настройки общих моков
    private void setupCommonMocks() {
        when(fsm.getTempData(userId, "reminderName")).thenReturn("test_reminder");
    }

    @Test
    @DisplayName("Тест handle: успешное добавление повторяющегося напоминания (ПН-ПТ 09:00)")
    void testHandle_SuccessStandardFormat() {
        setupCommonMocks();

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingRecurringDays handler = new HandleAwaitingRecurringDays();

            // When
            handler.handle(userId, chatId, "ПН-ПТ 09:00", bot);

            // Then
            verify(reminderService).addRecurringReminder(eq(userId), eq("test_reminder"), any(RecurringProperties.class));
            verify(bot).sendMessage(eq(chatId), eq("Повторяющееся напоминание \"test_reminder\" добавлено на ПН-ПТ в 09:00."));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: успешное добавление с гибким форматом (ПН, СР 9:00)")
    void testHandle_SuccessFlexibleFormat() {
        setupCommonMocks();

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingRecurringDays handler = new HandleAwaitingRecurringDays();

            // When проверяем поддержку H:mm и перечисления через запятую
            handler.handle(userId, chatId, "ПН, СР 9:00", bot);

            // Then
            verify(reminderService).addRecurringReminder(eq(userId), eq("test_reminder"), any(RecurringProperties.class));
            verify(bot).sendMessage(eq(chatId), eq("Повторяющееся напоминание \"test_reminder\" добавлено на ПН, СР в 9:00."));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: пустой или пробельный ввод")
    void testHandle_EmptyInput() {
        setupCommonMocks();

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingRecurringDays handler = new HandleAwaitingRecurringDays();

            // When
            handler.handle(userId, chatId, "   ", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Дни недели и время не могут быть пустыми."));
            verify(reminderService, never()).addRecurringReminder(anyLong(), anyString(), any());
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: отсутствует пробел между днями и временем")
    void testHandle_MissingSpace() {
        setupCommonMocks();

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingRecurringDays handler = new HandleAwaitingRecurringDays();

            // When
            handler.handle(userId, chatId, "ПН-ПТ09:00", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), contains("Неверный формат. Используйте: Дни Время"));
            verify(reminderService, never()).addRecurringReminder(anyLong(), anyString(), any());
            verify(fsm, never()).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: неверный формат времени (25:00)")
    void testHandle_InvalidTimeFormat() {
        setupCommonMocks();

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingRecurringDays handler = new HandleAwaitingRecurringDays();

            // When
            handler.handle(userId, chatId, "ПН 25:00", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), contains("Неверный формат времени"));
            verify(reminderService, never()).addRecurringReminder(anyLong(), anyString(), any());
            verify(fsm, never()).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: неверный формат дней (XYZ)")
    void testHandle_InvalidDaysFormat() {
        setupCommonMocks();

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingRecurringDays handler = new HandleAwaitingRecurringDays();

            // When
            handler.handle(userId, chatId, "XYZ 09:00", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), contains("Неверный формат дней"));
            verify(reminderService, never()).addRecurringReminder(anyLong(), anyString(), any());
            verify(fsm, never()).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: ошибка БД - имя напоминания уже существует")
    void testHandle_DbErrorNameExists() {
        setupCommonMocks();
        
        doThrow(new RuntimeException("Напоминание с именем 'test_reminder' уже существует"))
                .when(reminderService).addRecurringReminder(anyLong(), anyString(), any());

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingRecurringDays handler = new HandleAwaitingRecurringDays();

            // When
            handler.handle(userId, chatId, "ПН 09:00", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), contains("Напоминание с именем \"test_reminder\" уже существует"));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: общая ошибка БД при добавлении")
    void testHandle_GeneralDbError() {
        setupCommonMocks();
        
        doThrow(new RuntimeException("Database connection lost"))
                .when(reminderService).addRecurringReminder(anyLong(), anyString(), any());

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingRecurringDays handler = new HandleAwaitingRecurringDays();

            // When
            handler.handle(userId, chatId, "ПН 09:00", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Ошибка при добавлении напоминания. Попробуйте позже."));
            // Ошибка произошла внутри try, поэтому сработает finally
            verify(fsm).resetState(userId);
        }
    }
}