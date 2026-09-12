package bot.commands.reminders;

import bot.AbstractBotTest;
import bot.fsm.BotFSM;
import bot.fsm.UserState;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

class AddRecurringReminderCommandTest extends AbstractBotTest {

    @Test
    @DisplayName("Тест execute: отправляет сообщение и устанавливает состояние FSM")
    void testExecuteSetsStateAndTempData() {
        try (MockedStatic<BotFSM> mockedBotFSM = Mockito.mockStatic(BotFSM.class)) {
            mockedBotFSM.when(BotFSM::getInstance).thenReturn(fsm);
            
            AddRecurringReminderCommand command = new AddRecurringReminderCommand();
            
            // When
            command.execute(bot, message, new String[]{});

            // Then
            verify(bot).sendMessage(chatId, "Введите имя напоминания:");
            verify(fsm).setState(userId, UserState.AWAITING_REMINDER_NAME_TO_ADD);
            verify(fsm).setTempData(userId, "command", "addRecurringReminder");
        } 
    }

    @Test
    @DisplayName("Тест геттеров")
    void testGetters() {
        AddRecurringReminderCommand command = new AddRecurringReminderCommand();
        
        assertEquals("addRecurringReminder", command.getCommandName());
        assertEquals("Добавить повторяющееся напоминание", command.getDescription());
        assertEquals("/addRecurringReminder", command.getUsage());
    }
}