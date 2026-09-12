package bot;

import bot.commands.Command;
import bot.fsm.BotFSM;
import bot.fsm.UserState;
import bot.handlers.notes.*;
import bot.handlers.reminders.*;
import bot.handlers.timezone.*;
import bot.reminder.Reminder;
import bot.reminder.ReminderService;
import bot.reminder.ReminderType;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class TelegramBot extends TelegramLongPollingBot {

	private final String botToken = System.getenv("TELEGRAM_BOT_TOKEN");
	private final CommandRegistry commandRegistry;
	private final ReminderService reminderService = ReminderService.INSTANCE;
	private final BotFSM fsm = BotFSM.INSTANCE;

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	private static final HandleAwaitingNoteNameToAdd handleAwaitingNoteNameToAdd = new HandleAwaitingNoteNameToAdd();
	private static final HandleAwaitingNoteText handleAwaitingNoteText = new HandleAwaitingNoteText();
	private static final HandleAwaitingNoteNameToEdit handleAwaitingNoteNameToEdit = new HandleAwaitingNoteNameToEdit();
	private static final HandleAwaitingNoteNewName handleAwaitingNoteNewName = new HandleAwaitingNoteNewName();
	private static final HandleAwaitingNoteNewText handleAwaitingNoteNewText = new HandleAwaitingNoteNewText();
	private static final HandleAwaitingNoteNameToShow handleAwaitingNoteNameToShow = new HandleAwaitingNoteNameToShow();
	private static final HandleAwaitingNoteNameToRemove handleAwaitingNoteNameToRemove = new HandleAwaitingNoteNameToRemove();
	private static final HandleAwaitingReminderNameToAdd handleAwaitingReminderNameToAdd = new HandleAwaitingReminderNameToAdd();
	private static final HandleAwaitingOnceDate handleAwaitingOnceDate = new HandleAwaitingOnceDate();
	private static final HandleAwaitingRecurringDays handleAwaitingRecurringDays = new HandleAwaitingRecurringDays();
	private static final HandleAwaitingReminderNameToEdit handleAwaitingReminderNameToEdit = new HandleAwaitingReminderNameToEdit();
	private static final HandleAwaitingReminderNewName handleAwaitingReminderNewName = new HandleAwaitingReminderNewName();
	private static final HandleAwaitingReminderNewTime handleAwaitingReminderNewTime = new HandleAwaitingReminderNewTime();
	private static final HandleAwaitingReminderNameToRemove handleAwaitingReminderNameToRemove = new HandleAwaitingReminderNameToRemove();
	private static final HandleAwaitingReminderNameToShow handleAwaitingReminderNameToShow = new HandleAwaitingReminderNameToShow();
	private static final HandleAwaitingTimezoneOffset handleAwaitingTimezoneOffset = new HandleAwaitingTimezoneOffset();

	public TelegramBot(CommandRegistry registry) {
		this.commandRegistry = registry;
		// Ежеминутная проверка напоминаний
		scheduler.scheduleAtFixedRate(this::checkAndSendReminders, 0, 1, TimeUnit.MINUTES);
	}

	@Override
	public String getBotUsername() {
		return "Unterrichtung_bot";
	}

	@Override
	public String getBotToken() {
		return botToken;
	}

	@Override
	public void onUpdateReceived(Update update) {
		if (!update.hasMessage())
			return;

		Message message = update.getMessage();
		Long userId = message.getFrom().getId();
		Long chatId = message.getChatId();

		if (!message.hasText()) {
			return; // Игнорируем не-текстовые сообщения
		}

		String text = message.getText().trim();

		// "/cancel"
		if (text.equals("/cancel")) {
			Command cancelCommand = commandRegistry.getCommand("cancel");
			if (cancelCommand != null) {
				cancelCommand.execute(this, message, new String[0]);
			}
			return;
		}

		// Получаем текущее состояние пользователя
		UserState state = fsm.getState(userId);

		// Обрабатываем ввод в зависимости от состояния
		switch (state) {
		case DEFAULT:
			handleDefaultState(chatId, text, message);
			break;
		// Notes
		case AWAITING_NOTE_NAME_TO_ADD:
			handleAwaitingNoteNameToAdd.handle(userId, chatId, text, this);
			break;
		case AWAITING_NOTE_TEXT:
			handleAwaitingNoteText.handle(userId, chatId, text, this);
			break;
		case AWAITING_NOTE_NAME_TO_EDIT:
			handleAwaitingNoteNameToEdit.handle(userId, chatId, text, this);
			break;
		case AWAITING_NOTE_NEW_NAME:
			handleAwaitingNoteNewName.handle(userId, chatId, text, this);
			break;
		case AWAITING_NOTE_NEW_TEXT:
			handleAwaitingNoteNewText.handle(userId, chatId, text, this);
			break;
		case AWAITING_NOTE_NAME_TO_SHOW:
			handleAwaitingNoteNameToShow.handle(userId, chatId, text, this);
			break;
		case AWAITING_NOTE_NAME_TO_REMOVE:
			handleAwaitingNoteNameToRemove.handle(userId, chatId, text, this);
			break;
		// Reminders
		case AWAITING_REMINDER_NAME_TO_ADD:
			handleAwaitingReminderNameToAdd.handle(userId, chatId, text, this);
			break;
		case AWAITING_ONCE_DATE:
			handleAwaitingOnceDate.handle(userId, chatId, text, this);
			break;
		case AWAITING_RECURRING_DAYS:
			handleAwaitingRecurringDays.handle(userId, chatId, text, this);
			break;
		case AWAITING_REMINDER_NAME_TO_EDIT:
			handleAwaitingReminderNameToEdit.handle(userId, chatId, text, this);
			break;
		case AWAITING_REMINDER_NEW_NAME:
			handleAwaitingReminderNewName.handle(userId, chatId, text, this);
			break;
		case AWAITING_REMINDER_NEW_TIME:
			handleAwaitingReminderNewTime.handle(userId, chatId, text, this);
			break;
		case AWAITING_REMINDER_NAME_TO_REMOVE:
			handleAwaitingReminderNameToRemove.handle(userId, chatId, text, this);
			break;
		case AWAITING_REMINDER_NAME_TO_SHOW:
			handleAwaitingReminderNameToShow.handle(userId, chatId, text, this);
			break;
		// Timezone
		case AWAITING_TIMEZONE_OFFSET:
			handleAwaitingTimezoneOffset.handle(userId, chatId, text, this);
			break;
			
		default:
			break;
		}
	}

	private void handleDefaultState(Long chatId, String text, Message message) {
		if (text.startsWith("/")) {
			String[] parts = text.split("\\s+", 2);
			String cmdName = parts[0].substring(1); // убираем "/"
			String[] args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];

			Command command = commandRegistry.getCommand(cmdName);
			if (command != null) {
				command.execute(this, message, args);
			} else {
				sendMessage(chatId, "Неизвестная команда. Введите /help для списка команд.");
			}
		} else {
			sendMessage(chatId, "Введите команду, например, /help.");
		}
	}

	// Вспомогательные команды
	public void sendMessage(Long chatId, String text) {
		org.telegram.telegrambots.meta.api.methods.send.SendMessage msg = org.telegram.telegrambots.meta.api.methods.send.SendMessage
				.builder().chatId(chatId.toString()).text(text).build();
		try {
			execute(msg);
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}
	private void checkAndSendReminders() {
		System.out.println("[" + java.time.LocalDateTime.now() + "] Запущен checkAndSend");

		try {
			List<Reminder> due = reminderService.getDueReminders();
			System.out.println("Найдено напоминаний для отправки: " + due.size());

			for (Reminder reminder : due) {
				String message = "Напоминание: " + reminder.getName();

				try {
					sendMessage(reminder.getUserId(), message);
					System.out.println("Успешно отправлено пользователю " + reminder.getUserId());
				} catch (Exception sendEx) {
					System.err.println("Ошибка отправки сообщения пользователю " + reminder.getUserId() + ": "
							+ sendEx.getMessage());
				}

				// Если это разовое напоминание — удаляем его из базы
				if (ReminderType.ONCE.equals(reminder.getType())) {
					try {
						reminderService.removeReminder(reminder.getUserId(), reminder.getName());
						System.out.println("Разовое напоминание '" + reminder.getName() + "' удалено из базы.");
					} catch (Exception removeEx) {
						System.err.println("Ошибка удаления напоминания: " + removeEx.getMessage());
					}
				}
			}
		} catch (Throwable e) {
			System.err.println("КРИТИЧЕСКАЯ ОШИБКА в потоке планировщика напоминаний:");
			e.printStackTrace();
		}
	}

	public void onDestroy() {
		scheduler.shutdown();
		try {
			if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
				// если за 5 секунд планировщик не остановился, то принудительно останавливаем
				scheduler.shutdownNow();
			}
		} catch (InterruptedException e) {
			scheduler.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}