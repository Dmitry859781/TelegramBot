package bot.handlers.reminders;

import bot.AbstractBotTest;
import bot.fsm.BotFSM;
import bot.fsm.UserState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HandleAwaitingReminderNameToAddTest extends AbstractBotTest {

    @Mock
    private BotFSM fsm;

    @Test
    @DisplayName("Тест handle: пустой или пробельный ввод")
    void testHandle_EmptyInput() {
        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingReminderNameToAdd handler = new HandleAwaitingReminderNameToAdd();

            // When
            handler.handle(userId, chatId, "   ", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Имя напоминания не может быть пустым. Попробуйте снова или введите /cancel."));
            verify(fsm, never()).setTempData(anyLong(), anyString(), any());
            verify(fsm, never()).setState(anyLong(), any());
            verify(fsm, never()).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: команда addOnceReminder - переход в состояние ожидания даты")
    void testHandle_AddOnceReminder() {
        when(fsm.getTempData(userId, "command")).thenReturn("addOnceReminder");

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingReminderNameToAdd handler = new HandleAwaitingReminderNameToAdd();

            // When
            handler.handle(userId, chatId, "  Встреча  ", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), contains("Введите дату и время для разового напоминания"));
            verify(fsm).setState(userId, UserState.AWAITING_ONCE_DATE);
            verify(fsm).setTempData(userId, "reminderName", "Встреча");
        }
    }

    @Test
    @DisplayName("Тест handle: команда addRecurringReminder - переход в состояние ожидания дней")
    void testHandle_AddRecurringReminder() {
        when(fsm.getTempData(userId, "command")).thenReturn("addRecurringReminder");

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingReminderNameToAdd handler = new HandleAwaitingReminderNameToAdd();

            // When
            handler.handle(userId, chatId, "Тренировка", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), contains("Введите дни недели и время для повторяющегося напоминания"));
            verify(fsm).setState(userId, UserState.AWAITING_RECURRING_DAYS);
            verify(fsm).setTempData(userId, "reminderName", "Тренировка");
        }
    }

    @Test
    @DisplayName("Тест handle: неизвестная команда - сброс состояния и сообщение об ошибке")
    void testHandle_UnknownCommand() {
        when(fsm.getTempData(userId, "command")).thenReturn("someUnknownCommand");

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingReminderNameToAdd handler = new HandleAwaitingReminderNameToAdd();

            // When
            handler.handle(userId, chatId, "Напоминание", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Произошла внутренняя ошибка инициализации. Пожалуйста, начните заново."));
            verify(fsm).resetState(userId);
            verify(fsm, never()).setState(anyLong(), any());
            verify(fsm, never()).setTempData(anyLong(), anyString(), anyString());
        }
    }

    @Test
    @DisplayName("Тест handle: команда null - сброс состояния и сообщение об ошибке")
    void testHandle_NullCommand() {
        when(fsm.getTempData(userId, "command")).thenReturn(null);

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingReminderNameToAdd handler = new HandleAwaitingReminderNameToAdd();

            // When
            handler.handle(userId, chatId, "Напоминание", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Произошла внутренняя ошибка инициализации. Пожалуйста, начните заново."));
            verify(fsm).resetState(userId);
            verify(fsm, never()).setState(anyLong(), any());
            verify(fsm, never()).setTempData(anyLong(), anyString(), anyString());
        }
    }
}