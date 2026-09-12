package bot.handlers.reminders;

import bot.TelegramBot;
import bot.fsm.BotFSM;
import bot.handlers.StateHandler;
import bot.reminder.ReminderService;
import bot.reminder.once.OnceProperties;
import bot.reminder.recurring.RecurringProperties;
import bot.reminder.recurring.ScheduleItem;
import bot.timezone.UserTimezoneService;
import com.google.gson.Gson;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

public class HandleAwaitingReminderNewTime implements StateHandler {

    private final BotFSM fsm = BotFSM.getInstance();
    private final ReminderService reminderService = ReminderService.getInstance();
    private final UserTimezoneService timezoneService = UserTimezoneService.getInstance();
    private final Gson gson = new Gson();

    @Override
    public void handle(Long userId, Long chatId, String text, TelegramBot bot) {
        if (text == null || text.trim().isEmpty()) {
            bot.sendMessage(chatId, "Ввод не может быть пустым. Попробуйте снова или введите /cancel.");
            return;
        }

        String input = text.trim();
        String oldName = (String) fsm.getTempData(userId, "oldReminderName");
        String newName = (String) fsm.getTempData(userId, "newReminderName");
        String oldPropertiesJson = (String) fsm.getTempData(userId, "oldPropertiesJson");
        String reminderType = (String) fsm.getTempData(userId, "reminderType");

        String finalPropertiesJson;

        if ("-".equals(input)) {
            finalPropertiesJson = oldPropertiesJson;
            saveAndFinish(userId, chatId, bot, oldName, newName, finalPropertiesJson);
            return;
        }

        try {
            if ("ONCE".equals(reminderType)) {
                finalPropertiesJson = parseOnceDate(input, userId);
            } else {
                finalPropertiesJson = parseRecurringSchedule(input);
            }
            
            if (finalPropertiesJson == null) {
                bot.sendMessage(chatId, "Неверный формат. Попробуйте снова или введите /cancel.");
                return;
            }

            saveAndFinish(userId, chatId, bot, oldName, newName, finalPropertiesJson);

        } catch (Exception e) {
            e.printStackTrace();
            bot.sendMessage(chatId, "Произошла ошибка при обработке времени. Попробуйте снова или введите /cancel.");
        }
    }

    private String parseOnceDate(String input, Long userId) {
        ZoneId userZone = getUserZone(userId);
        LocalDateTime now = LocalDateTime.now(userZone);
        int currentYear = now.getYear();

        List<DateTimeFormatter> formattersWithYear = Arrays.asList(
                DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"),
                DateTimeFormatter.ofPattern("dd MM yyyy HH:mm")
        );

        List<DateTimeFormatter> formattersWithoutYear = Arrays.asList(
                new DateTimeFormatterBuilder().appendPattern("dd-MM HH:mm").parseDefaulting(ChronoField.YEAR, currentYear).toFormatter(),
                new DateTimeFormatterBuilder().appendPattern("dd MM HH:mm").parseDefaulting(ChronoField.YEAR, currentYear).toFormatter()
        );

        LocalDateTime parsedDateTime = null;
        boolean hasYearInInput = false;

        for (DateTimeFormatter formatter : formattersWithYear) {
            try {
                parsedDateTime = LocalDateTime.parse(input, formatter);
                hasYearInInput = true;
                break;
            } catch (DateTimeParseException ignored) {
            }
        }

        if (parsedDateTime == null) {
            for (DateTimeFormatter formatter : formattersWithoutYear) {
                try {
                    parsedDateTime = LocalDateTime.parse(input, formatter);
                    hasYearInInput = false;
                    break;
                } catch (DateTimeParseException ignored) {
                }
            }
        }

        if (parsedDateTime == null) {
            return null;
        }

        if (!hasYearInInput && parsedDateTime.isBefore(now)) {
            parsedDateTime = parsedDateTime.plusYears(1);
        }

        if (parsedDateTime.isBefore(now)) {
            return null;
        }

        OnceProperties props = new OnceProperties();
        LocalDateTime utcTime = parsedDateTime.atZone(userZone)
        						.withZoneSameInstant(ZoneOffset.UTC)
        						.toLocalDateTime();
        props.remind_at = utcTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return gson.toJson(props);
    }

    private String parseRecurringSchedule(String input) {
        int lastSpaceIndex = input.lastIndexOf(' ');
        if (lastSpaceIndex == -1) {
            return null;
        }

        String daysPart = input.substring(0, lastSpaceIndex).trim();
        String timePart = input.substring(lastSpaceIndex + 1).trim();

        try {
            LocalTime.parse(timePart, DateTimeFormatter.ofPattern("H:mm"));
        } catch (DateTimeParseException e) {
            return null;
        }

        List<String> dayCodes = parseDays(daysPart);
        if (dayCodes.isEmpty()) {
            return null;
        }

        RecurringProperties props = new RecurringProperties();
        List<ScheduleItem> schedule = new ArrayList<>();
        for (String dayCode : dayCodes) {
            ScheduleItem item = new ScheduleItem();
            item.day = dayCode; // Здесь будет "MON", "TUE" и т.д.
            item.time = timePart;
            schedule.add(item);
        }
        props.schedule = schedule;

        return gson.toJson(props);
    }

    private List<String> parseDays(String input) {
        List<String> result = new ArrayList<>();
        input = input.replace('–', '-').replace('—', '-');
        String[] rangesOrItems = input.split(",");

        for (String rangeOrItem : rangesOrItems) {
            rangeOrItem = rangeOrItem.trim();
            if (rangeOrItem.isEmpty()) continue;

            if (rangeOrItem.contains("-")) {
                String[] range = rangeOrItem.split("-", 2);
                String startDay = range[0].trim();
                String endDay = range[1].trim();

                List<String> daysInRange = getDaysInRange(startDay, endDay);
                if (daysInRange.isEmpty()) {
                    return new ArrayList<>(); 
                }
                result.addAll(daysInRange);
            } else {
                DayOfWeek dow = resolveDayOfWeek(rangeOrItem);
                if (dow == null) {
                    return new ArrayList<>();
                }
                result.add(dow.toString().substring(0, 3));
            }
        }

        return new ArrayList<>(new LinkedHashSet<>(result));
    }

    private List<String> getDaysInRange(String start, String end) {
        DayOfWeek startDow = resolveDayOfWeek(start);
        if (startDow == null) return new ArrayList<>();

        DayOfWeek endDow = resolveDayOfWeek(end);
        if (endDow == null) return new ArrayList<>();

        List<String> result = new ArrayList<>();
        DayOfWeek current = startDow;
        while (true) {
            result.add(current.toString().substring(0, 3));
            if (current == endDow) {
                break;
            }
            current = current.plus(1);
        }

        return result;
    }

    private DayOfWeek resolveDayOfWeek(String dayStr) {
        String upper = dayStr.trim().toUpperCase();
        
        try {
            return DayOfWeek.valueOf(upper);
        } catch (IllegalArgumentException ignored) {
        }

        switch (upper) {
            case "MON": case "ПН": return DayOfWeek.MONDAY;
            case "TUE": case "ВТ": return DayOfWeek.TUESDAY;
            case "WED": case "СР": return DayOfWeek.WEDNESDAY;
            case "THU": case "ЧТ": return DayOfWeek.THURSDAY;
            case "FRI": case "ПТ": return DayOfWeek.FRIDAY;
            case "SAT": case "СБ": return DayOfWeek.SATURDAY;
            case "SUN": case "ВС": return DayOfWeek.SUNDAY;
            default: return null;
        }
    }

    private void saveAndFinish(Long userId, Long chatId, TelegramBot bot, String oldName, String newName, String finalPropertiesJson) {
        try {
            reminderService.updateReminder(userId, oldName, newName, finalPropertiesJson);
            bot.sendMessage(chatId, "Напоминание \"" + newName + "\" успешно обновлено!");
        } catch (SQLException e) {
            if (e.getMessage() != null && (e.getMessage().contains("UNIQUE constraint failed") || e.getMessage().contains("уже существует"))) {
                bot.sendMessage(chatId, "Напоминание с именем \"" + newName + "\" уже существует. Операция отменена.");
            } else {
                e.printStackTrace();
                bot.sendMessage(chatId, "Ошибка при сохранении изменений в базе данных.");
            }
        } finally {
            cleanupTempData(userId);
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

    private void cleanupTempData(Long userId) {
        fsm.clearTempData(userId, "oldReminderName");
        fsm.clearTempData(userId, "newReminderName");
        fsm.clearTempData(userId, "oldPropertiesJson");
        fsm.clearTempData(userId, "reminderType");
    }
}