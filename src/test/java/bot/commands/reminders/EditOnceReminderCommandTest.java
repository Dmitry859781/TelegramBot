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

class EditOnceReminderCommandTest extends AbstractBotTest {
    @Mock
    private ReminderService reminderService;

    @Test
    @DisplayName("Тест execute: напоминания есть, выводит список и меняет состояние")
    void testExecute_HasReminders() throws SQLException {
        List<String> mockNames = List.of("Встреча", "Полив цветов");
		when(reminderService.getUserReminderNames(userId, "ONCE")).thenReturn(mockNames);
        
        try (MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {
            
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            EditOnceReminderCommand command = new EditOnceReminderCommand();
            
            // When
            command.execute(bot, message, new String[]{});

            // Then
            verify(bot).sendMessage(chatId, "Какое напоминание редактировать?\n1. Встреча\n2. Полив цветов\n");
            verify(bot).sendMessage(chatId, "Введите имя напоминания для редактирования:");
            verify(fsm).setState(userId, UserState.AWAITING_REMINDER_NAME_TO_EDIT);
            verify(fsm).setTempData(userId, "command", "editOnceReminder");
        }
    }

    @Test
    @DisplayName("Тест execute: напоминаний нет, выводит сообщение и НЕ меняет состояние")
    void testExecute_EmptyReminders() throws SQLException {
    	when(reminderService.getUserReminderNames(userId, "ONCE")).thenReturn(List.of());

        try (MockedStatic<ReminderService> mockedRs = Mockito.mockStatic(ReminderService.class);
             MockedStatic<BotFSM> mockedFsm = Mockito.mockStatic(BotFSM.class)) {
            
            mockedRs.when(ReminderService::getInstance).thenReturn(reminderService);
            mockedFsm.when(BotFSM::getInstance).thenReturn(fsm);

            EditOnceReminderCommand command = new EditOnceReminderCommand();
            
            // When
            command.execute(bot, message, new String[]{});

            // Then
            verify(bot).sendMessage(chatId, "У вас нет напоминаний. Добавьте сначала через /addOnceReminder или /addRecurringReminder.");
            verify(fsm, Mockito.never()).setState(Mockito.anyLong(), Mockito.any());
            verify(fsm, Mockito.never()).setTempData(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString());
        }
    }

    @Test
    @DisplayName("Тест геттеров")
    void testGetters() {
        EditOnceReminderCommand command = new EditOnceReminderCommand();
        
        assertEquals("editOnceReminder", command.getCommandName());
        assertEquals("Редактировать разовое напоминание", command.getDescription());
        assertEquals("/editOnceReminder", command.getUsage());
    }
}