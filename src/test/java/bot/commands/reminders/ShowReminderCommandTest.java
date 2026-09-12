package bot.commands.reminders;

import bot.AbstractBotTest;
import bot.fsm.BotFSM;
import bot.fsm.UserState;
import bot.reminder.ReminderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShowReminderCommandTest extends AbstractBotTest {

    @Mock
    private ReminderService reminderService;

    @Test
    @DisplayName("Тест execute: напоминания есть, выводит список и меняет состояние")
    void testExecute_HasReminders() throws SQLException {
        when(reminderService.getUserReminderNames(userId, null)).thenReturn(List.of("Завтрак", "Обед"));

        try (MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            ShowReminderCommand command = new ShowReminderCommand();

            // When
            command.execute(bot, message, new String[]{});

            // Then
            verify(bot).sendMessage(chatId, "Какое напоминание показать?\n1. Завтрак\n2. Обед\n");
            verify(bot).sendMessage(chatId, "Введите имя напоминания:");
            verify(fsm).setState(userId, UserState.AWAITING_REMINDER_NAME_TO_SHOW);
            verify(fsm).setTempData(userId, "command", "showReminder");
        }
    }

    @Test
    @DisplayName("Тест execute: напоминаний нет, выводит сообщение и НЕ меняет состояние")
    void testExecute_EmptyReminders() throws SQLException {
        when(reminderService.getUserReminderNames(userId, null)).thenReturn(List.of());

        try (MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            ShowReminderCommand command = new ShowReminderCommand();

            // When
            command.execute(bot, message, new String[]{});

            // Then
            verify(bot).sendMessage(chatId, "У вас нет напоминаний. Добавьте сначала через /addOnceReminder или /addRecurringReminder.");
            verify(fsm, Mockito.never()).setState(Mockito.anyLong(), Mockito.any());
            verify(fsm, Mockito.never()).setTempData(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString());
        }
    }

    @Test
    @DisplayName("Тест execute: обработка исключения при получении списка")
    void testExecute_Exception() throws SQLException {
        when(reminderService.getUserReminderNames(userId, null)).thenThrow(new RuntimeException("DB error"));

        try (MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {

            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            ShowReminderCommand command = new ShowReminderCommand();

            // When
            command.execute(bot, message, new String[]{});

            // Then
            verify(bot).sendMessage(chatId, "Ошибка при получении списка напоминаний. Попробуйте позже.");
            verify(fsm, Mockito.never()).setState(Mockito.anyLong(), Mockito.any());
        }
    }

    @Test
    @DisplayName("Тест геттеров")
    void testGetters() {
        ShowReminderCommand command = new ShowReminderCommand();

        assertEquals("showReminder", command.getCommandName());
        assertEquals("Показать напоминание", command.getDescription());
        assertEquals("/showReminder", command.getUsage());
    }
}