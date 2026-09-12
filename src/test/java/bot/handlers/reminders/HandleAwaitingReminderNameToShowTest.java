package bot.handlers.reminders;

import bot.AbstractBotTest;
import bot.fsm.BotFSM;
import bot.reminder.Reminder;
import bot.reminder.ReminderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HandleAwaitingReminderNameToShowTest extends AbstractBotTest {

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

            HandleAwaitingReminderNameToShow handler = new HandleAwaitingReminderNameToShow();

            // When
            handler.handle(userId, chatId, "   ", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Имя напоминания не может быть пустым. Попробуйте снова или введите /cancel."));
            verify(reminderService, never()).getReminder(anyLong(), anyString());
            verify(fsm, never()).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: успешный вывод имени напоминания")
    void testHandle_Success() throws SQLException {
        // Given настраиваем мок объекта Reminder
        when(reminder.getName()).thenReturn("Мое напоминание");
        when(reminderService.getReminder(userId, "my_reminder")).thenReturn(reminder);

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingReminderNameToShow handler = new HandleAwaitingReminderNameToShow();

            // When передаем имя с пробелами для проверки trim()
            handler.handle(userId, chatId, "  my_reminder  ", bot);

            // Then
            verify(reminderService).getReminder(userId, "my_reminder");
            verify(bot).sendMessage(eq(chatId), eq("Мое напоминание"));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: напоминание не найдено")
    void testHandle_ReminderNotFound() throws SQLException {
        // Given сервис возвращает null
        when(reminderService.getReminder(userId, "missing_name")).thenReturn(null);

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingReminderNameToShow handler = new HandleAwaitingReminderNameToShow();

            // When
            handler.handle(userId, chatId, "missing_name", bot);

            // Then
            verify(reminderService).getReminder(userId, "missing_name");
            verify(bot).sendMessage(eq(chatId), eq("Напоминание \"missing_name\" не найдено."));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: ошибка базы данных при получении напоминания")
    void testHandle_DatabaseError() throws SQLException {
        // Given сервис выбрасывает SQLException
        when(reminderService.getReminder(userId, "error_name"))
                .thenThrow(new SQLException("DB connection failed"));

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingReminderNameToShow handler = new HandleAwaitingReminderNameToShow();

            // When
            handler.handle(userId, chatId, "error_name", bot);

            // Then
            verify(reminderService).getReminder(userId, "error_name");
            verify(bot).sendMessage(eq(chatId), eq("Ошибка при получении напоминания из базы данных."));
            verify(fsm).resetState(userId);
        }
    }
}