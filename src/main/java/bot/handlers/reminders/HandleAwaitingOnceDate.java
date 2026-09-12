package bot.handlers.reminders;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder; // <-- Добавлен этот импорт
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;           // <-- Добавлен этот импорт
import java.util.Arrays;
import java.util.List;

import bot.TelegramBot;
import bot.fsm.BotFSM;
import bot.handlers.StateHandler;
import bot.reminder.ReminderService;
import bot.timezone.UserTimezoneService;

public class HandleAwaitingOnceDate implements StateHandler {
    private final UserTimezoneService timezoneService = UserTimezoneService.getInstance(); 
    private final BotFSM fsm = BotFSM.getInstance();
    private final ReminderService reminderService = ReminderService.getInstance();
    
    public void handle(Long userId, Long chatId, String text, TelegramBot bot) {
        String reminderName = (String) fsm.getTempData(userId, "reminderName");

        if (text == null || text.trim().isEmpty()) {
            bot.sendMessage(chatId, "Дата и время не могут быть пустыми.");
            fsm.resetState(userId);
            return;
        }

        String input = text.trim();
        LocalDateTime parsedDateTime = null;
        boolean hasYearInInput = false;
        
        ZoneId userZone = getUserZone(userId);
        LocalDateTime now = LocalDateTime.now(userZone);
        int currentYear = now.getYear();
        
        // Год указан
        List<DateTimeFormatter> formattersWithYear = Arrays.asList(
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd MM yyyy HH:mm")
        );

        // Год не указан
        List<DateTimeFormatter> formattersWithoutYear = Arrays.asList(
            new DateTimeFormatterBuilder()
                .appendPattern("dd-MM HH:mm")
                .parseDefaulting(ChronoField.YEAR, currentYear)
                .toFormatter(),
            new DateTimeFormatterBuilder()
                .appendPattern("dd MM HH:mm")
                .parseDefaulting(ChronoField.YEAR, currentYear)
                .toFormatter()
        );

        // Пытаемся распарсить сначала форматы с годом
        for (DateTimeFormatter formatter : formattersWithYear) {
            try {
                parsedDateTime = LocalDateTime.parse(input, formatter);
                hasYearInInput = true;
                break;
            } catch (DateTimeParseException ignored) {}
        }

        // Пытаемся распарсить форматы без года
        if (parsedDateTime == null) {
            for (DateTimeFormatter formatter : formattersWithoutYear) {
                try {
                    parsedDateTime = LocalDateTime.parse(input, formatter);
                    hasYearInInput = false;
                    break;
                } catch (DateTimeParseException ignored) {}
            }
        }

        if (parsedDateTime == null) {
            bot.sendMessage(chatId, "Неверный формат даты/времени. Используйте форматы: ДД-ММ-ГГГГ ЧЧ:ММ, ДД-ММ ЧЧ:ММ, ДД ММ ГГГГ ЧЧ:ММ или ДД ММ ЧЧ:ММ. Попробуйте снова или введите /cancel.");
            return;
        }

        // Если года не было в вводе и полученная дата уже прошла в этом году значит пользователь имеет в виду следующий год
        if (!hasYearInInput && parsedDateTime.isBefore(now)) {
            parsedDateTime = parsedDateTime.plusYears(1);
        }

        // Проверка на не в прошлом ли дата
        if (parsedDateTime.isBefore(now)) {
            bot.sendMessage(chatId, "Дата и время не могут быть в прошлом. Укажите будущее время.");
            return;
        }

        try {
            reminderService.addOnceReminder(userId, reminderName, parsedDateTime, userZone);
            bot.sendMessage(chatId, "Разовое напоминание \"" + reminderName + "\" добавлено на " + 
                parsedDateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + ".");
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("уже существует")) {
                bot.sendMessage(chatId, "Напоминание с именем \"" + reminderName + "\" уже существует. Пожалуйста, начните создание заново и выберите другое имя.");
            } else {
                e.printStackTrace();
                bot.sendMessage(chatId, "Ошибка при добавлении напоминания. Попробуйте позже.");
            }
        } finally {
            fsm.resetState(userId);
        }
    }
    
    private ZoneId getUserZone(Long userId) {
        try {
            Integer offsetHours = timezoneService.getTimezone(userId);
            if (offsetHours != null) {
                return ZoneOffset.ofHours(offsetHours);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ZoneId.systemDefault();
    }
}