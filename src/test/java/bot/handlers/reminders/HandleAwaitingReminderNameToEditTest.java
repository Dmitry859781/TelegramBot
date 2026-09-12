package bot.handlers.reminders;

import bot.AbstractBotTest;
import bot.fsm.BotFSM;
import bot.fsm.UserState;
import bot.reminder.Reminder;
import bot.reminder.ReminderService;
import bot.reminder.ReminderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HandleAwaitingReminderNameToEditTest extends AbstractBotTest {

    @Mock
    private BotFSM fsm;

    @Mock
    private ReminderService reminderService;

    @Mock
    private Reminder reminder;

    @Test
    @DisplayName("Тест handle: пустой или пробельный ввод")
    void testHandle_EmptyInput() throws SQLException {
        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingReminderNameToEdit handler = new HandleAwaitingReminderNameToEdit();

            // When
            handler.handle(userId, chatId, "   ", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Имя напоминания не может быть пустым. Попробуйте снова или введите /cancel."));
            verify(reminderService, never()).getReminder(anyLong(), anyString());
            verify(fsm, never()).setTempData(anyLong(), anyString(), any());
            verify(fsm, never()).setState(anyLong(), any());
            verify(fsm, never()).resetState(anyLong());
        }
    }

    @Test
    @DisplayName("Тест handle: напоминание с таким именем не найдено")
    void testHandle_ReminderNotFound() throws SQLException {
        // Given сервис возвращает null
        when(reminderService.getReminder(userId, "missing_name")).thenReturn(null);

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingReminderNameToEdit handler = new HandleAwaitingReminderNameToEdit();

            // When
            handler.handle(userId, chatId, "missing_name", bot);

            // Then
            verify(reminderService).getReminder(userId, "missing_name");
            verify(bot).sendMessage(eq(chatId), eq("Напоминание с именем \"missing_name\" не найдено. Проверьте имя или введите /cancel."));
            verify(fsm, never()).setTempData(anyLong(), anyString(), any());
            verify(fsm, never()).setState(anyLong(), any());
            verify(fsm, never()).resetState(anyLong());
        }
    }

    @Test
    @DisplayName("Тест handle: успешный поиск напоминания и переход к редактированию")
    void testHandle_Success() throws SQLException {
        // Given настраиваем мок объекта Reminder
        when(reminder.getType()).thenReturn(ReminderType.ONCE);
        when(reminder.getPropertiesJson()).thenReturn("{\"remind_at\":\"2024-01-01 10:00:00\"}");
        when(reminderService.getReminder(userId, "old_name")).thenReturn(reminder);

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingReminderNameToEdit handler = new HandleAwaitingReminderNameToEdit();

            // When передаем имя с пробелами для проверки trim()
            handler.handle(userId, chatId, "  old_name  ", bot);

            // Then
            verify(reminderService).getReminder(userId, "old_name");
            verify(fsm).setTempData(userId, "oldReminderName", "old_name");
            verify(fsm).setTempData(userId, "oldPropertiesJson", "{\"remind_at\":\"2024-01-01 10:00:00\"}");
            verify(fsm).setTempData(userId, "reminderType", "ONCE");
            verify(fsm).setState(userId, UserState.AWAITING_REMINDER_NEW_NAME);
            verify(bot).sendMessage(eq(chatId), eq("Текущее имя: \"old_name\"\n\nВведите новое имя напоминания (или отправьте `-`, чтобы оставить без изменений):"));
        }
    }

    @Test
    @DisplayName("Тест handle: ошибка базы данных при поиске напоминания")
    void testHandle_DatabaseError() throws SQLException {
        // Given сервис выбрасывает SQLException
        when(reminderService.getReminder(userId, "error_name")).thenThrow(new SQLException("DB connection failed"));

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingReminderNameToEdit handler = new HandleAwaitingReminderNameToEdit();

            // When
            handler.handle(userId, chatId, "error_name", bot);

            // Then
            verify(reminderService).getReminder(userId, "error_name");
            verify(bot).sendMessage(eq(chatId), eq("Ошибка при поиске напоминания. Попробуйте позже."));
            verify(fsm).resetState(userId);
            verify(fsm, never()).setTempData(anyLong(), anyString(), any());
            verify(fsm, never()).setState(anyLong(), any());
        }
    }
}