package bot.commands.reminders;

import bot.AbstractBotTest;
import bot.fsm.BotFSM;
import bot.fsm.UserState;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddOnceReminderCommandTest extends AbstractBotTest{
    @Test
    @DisplayName("Тест execute: отправляет сообщение и устанавливает состояние FSM")
    void testExecuteSetsStateAndTempData() {
    	try (MockedStatic<BotFSM> mockedBotFSM = Mockito.mockStatic(BotFSM.class)) {
            mockedBotFSM.when(BotFSM::getInstance).thenReturn(fsm);
            
            AddOnceReminderCommand command = new AddOnceReminderCommand();
            
            // When
            command.execute(bot, message, new String[]{});

            // Then
            verify(bot).sendMessage(chatId, "Введите имя напоминания:");
            verify(fsm).setState(userId, UserState.AWAITING_REMINDER_NAME_TO_ADD);
            verify(fsm).setTempData(userId, "command", "addOnceReminder");
        } 
    }

    @Test
    @DisplayName("Тест геттеров")
    void testGetters() {
    	AddOnceReminderCommand command = new AddOnceReminderCommand();
    	
    	assertEquals("addOnceReminder", command.getCommandName());
        assertEquals("Добавить разовое напоминание", command.getDescription());
        assertEquals("/addOnceReminder", command.getUsage());
    }
}