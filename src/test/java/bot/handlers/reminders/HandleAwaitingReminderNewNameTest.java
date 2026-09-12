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

class HandleAwaitingReminderNewNameTest extends AbstractBotTest {

    @Mock
    private BotFSM fsm;

    // Вспомогательный метод для настройки общих моков
    private void setupCommonMocks(String oldName, String reminderType) {
        when(fsm.getTempData(userId, "oldReminderName")).thenReturn(oldName);
        when(fsm.getTempData(userId, "reminderType")).thenReturn(reminderType);
    }

    @Test
    @DisplayName("Тест handle: пустой или пробельный ввод")
    void testHandle_EmptyInput() {
        setupCommonMocks("old_name", "ONCE");

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingReminderNewName handler = new HandleAwaitingReminderNewName();

            // When
            handler.handle(userId, chatId, "   ", bot);

            // Then
            verify(bot).sendMessage(eq(chatId), eq("Ввод не может быть пустым. Попробуйте снова или введите /cancel."));
            verify(fsm, never()).setTempData(anyLong(), anyString(), anyString());
            verify(fsm, never()).setState(anyLong(), any());
            verify(fsm, never()).resetState(userId);
        }
    }

    @Test
    @DisplayName("Тест handle: ввод '-' для ONCE-напоминания (оставить старое имя)")
    void testHandle_DashForOnce() {
        setupCommonMocks("old_name", "ONCE");

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingReminderNewName handler = new HandleAwaitingReminderNewName();

            // When
            handler.handle(userId, chatId, " - ", bot);

            // Then
            verify(fsm).setTempData(userId, "newReminderName", "old_name");
            verify(fsm).setState(userId, UserState.AWAITING_REMINDER_NEW_TIME);
            verify(bot).sendMessage(eq(chatId), eq("Введите новую дату и время (например: 16-08-2026 15:30) или `-`, чтобы оставить старое время:"));
        }
    }

    @Test
    @DisplayName("Тест handle: ввод '-' для RECURRING-напоминания (оставить старое имя)")
    void testHandle_DashForRecurring() {
        setupCommonMocks("old_name", "RECURRING");

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingReminderNewName handler = new HandleAwaitingReminderNewName();

            // When
            handler.handle(userId, chatId, "-", bot);

            // Then
            verify(fsm).setTempData(userId, "newReminderName", "old_name");
            verify(fsm).setState(userId, UserState.AWAITING_REMINDER_NEW_TIME);
            verify(bot).sendMessage(eq(chatId), eq("Введите новые дни и время (например: ПН-ПТ 09:00) или `-`, чтобы оставить старое расписание:"));
        }
    }

    @Test
    @DisplayName("Тест handle: новое имя для ONCE-напоминания")
    void testHandle_NewNameForOnce() {
        setupCommonMocks("old_name", "ONCE");

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingReminderNewName handler = new HandleAwaitingReminderNewName();

            // When: передаем имя с пробелами
            handler.handle(userId, chatId, "  новое_имя  ", bot);

            // Then
            verify(fsm).setTempData(userId, "newReminderName", "новое_имя");
            verify(fsm).setState(userId, UserState.AWAITING_REMINDER_NEW_TIME);
            verify(bot).sendMessage(eq(chatId), eq("Введите новую дату и время (например: 16-08-2026 15:30) или `-`, чтобы оставить старое время:"));
        }
    }

    @Test
    @DisplayName("Тест handle: новое имя для RECURRING-напоминания")
    void testHandle_NewNameForRecurring() {
        setupCommonMocks("old_name", "RECURRING");

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingReminderNewName handler = new HandleAwaitingReminderNewName();

            // When
            handler.handle(userId, chatId, "повторяющееся_имя", bot);

            // Then
            verify(fsm).setTempData(userId, "newReminderName", "повторяющееся_имя");
            verify(fsm).setState(userId, UserState.AWAITING_REMINDER_NEW_TIME);
            verify(bot).sendMessage(eq(chatId), eq("Введите новые дни и время (например: ПН-ПТ 09:00) или `-`, чтобы оставить старое расписание:"));
        }
    }

    @Test
    @DisplayName("Тест handle: неизвестный тип напоминания - сброс состояния и сообщение об ошибке")
    void testHandle_UnknownReminderType() {
        setupCommonMocks("old_name", "UNKNOWN_TYPE");

        try (MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            HandleAwaitingReminderNewName handler = new HandleAwaitingReminderNewName();

            // When
            handler.handle(userId, chatId, "новое_имя", bot);

            // Then
            verify(fsm).setTempData(userId, "newReminderName", "новое_имя");
            verify(fsm).setState(userId, UserState.AWAITING_REMINDER_NEW_TIME);
            verify(bot).sendMessage(eq(chatId), eq("Произошла внутренняя ошибка инициализации. Пожалуйста, начните заново."));
            verify(fsm).resetState(userId);
        }
    }
}