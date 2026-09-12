package bot.handlers.reminders;

import bot.AbstractBotTest;
import bot.fsm.BotFSM;
import bot.reminder.ReminderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HandleAwaitingReminderNameToRemoveTest extends AbstractBotTest {

    @Mock
    private BotFSM fsm;

    @Mock
    private ReminderService reminderService;

    @Test
    @DisplayName("Тест handle: пустой или пробельный ввод")
    void testHandle_EmptyInput() throws SQLException {
        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingReminderNameToRemove handler = new HandleAwaitingReminderNameToRemove();

            // When
            handler.handle(userId, chatId, "   ", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Имя напоминания не может быть пустым. Попробуйте снова или введите /cancel."));
            verify(reminderService, never()).removeReminder(anyLong(), anyString());
            verify(fsm, never()).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: успешное удаление напоминания")
    void testHandle_Success() throws SQLException {
        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingReminderNameToRemove handler = new HandleAwaitingReminderNameToRemove();

            // When передаем имя с пробелами для проверки trim()
            handler.handle(userId, chatId, "  старое_напоминание  ", bot);

            // Then
            verify(reminderService).removeReminder(userId, "старое_напоминание");
            verify(bot).sendMessage(eq(chatId), eq("Напоминание \"старое_напоминание\" удалено."));
            verify(fsm).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: ошибка базы данных при удалении")
    void testHandle_DatabaseError() throws SQLException {
        // Given сервис выбрасывает SQLException
        doThrow(new SQLException("Reminder not found"))
                .when(reminderService).removeReminder(anyLong(), anyString());

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class);
             MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class)) {

            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);

            HandleAwaitingReminderNameToRemove handler = new HandleAwaitingReminderNameToRemove();

            // When
            handler.handle(userId, chatId, "несуществующее", bot);

            // Then
            verify(reminderService).removeReminder(userId, "несуществующее");
            verify(bot).sendMessage(eq(chatId), eq("Ошибка при удалении напоминания. Возможно, оно не существует или произошла ошибка базы данных."));
            verify(fsm).resetState(userId);
        }
    }
}