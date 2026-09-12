package bot.handlers.reminders;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import bot.TelegramBot;
import bot.fsm.BotFSM;
import bot.handlers.StateHandler;
import bot.reminder.ReminderService;
import bot.reminder.recurring.RecurringProperties;
import bot.reminder.recurring.ScheduleItem;

public class HandleAwaitingRecurringDays implements StateHandler{
	private final BotFSM fsm = BotFSM.getInstance();
	private final ReminderService reminderService = ReminderService.getInstance();
	
	public void handle(Long userId, Long chatId, String text, TelegramBot bot) {
        String reminderName = (String) fsm.getTempData(userId, "reminderName");

        if (text == null || text.trim().isEmpty()) {
            bot.sendMessage(chatId, "Дни недели и время не могут быть пустыми.");
            fsm.resetState(userId);
            return;
        }

        String trimmedText = text.trim();
        // Ищем последний пробел (он разделяет дни и время)
        int lastSpaceIndex = trimmedText.lastIndexOf(' ');

        if (lastSpaceIndex == -1) {
            bot.sendMessage(chatId, "Неверный формат. Используйте: Дни Время (например: ПН-ПТ 09:00 или ПН, ЧТ 9:00). Попробуйте снова или введите /cancel.");
            return;
        }

        String daysPart = trimmedText.substring(0, lastSpaceIndex).trim();
        String timePart = trimmedText.substring(lastSpaceIndex + 1).trim();

        try {
            LocalTime.parse(timePart, DateTimeFormatter.ofPattern("H:mm"));
        } catch (DateTimeParseException e) {
            bot.sendMessage(chatId, "Неверный формат времени. Используйте HH:MM или H:MM (например: 09:00 или 9:00). Попробуйте снова или введите /cancel.");
            return;
        }

        List<String> dayCodes = parseDays(daysPart);

        if (dayCodes.isEmpty()) {
            bot.sendMessage(chatId, "Неверный формат дней. Используйте: ПН, ПН-ПТ, MON, MON-FRI и т.д. Попробуйте снова или введите /cancel.");
            return;
        }

        // Создаём ScheduleItem для каждого дня
        List<ScheduleItem> scheduleItems = new ArrayList<>();
        for (String dayCode : dayCodes) {
            ScheduleItem item = new ScheduleItem();
            item.day = dayCode;
            item.time = timePart;
            scheduleItems.add(item);
        }

        RecurringProperties props = new RecurringProperties();
        props.schedule = scheduleItems;

        try {
            reminderService.addRecurringReminder(userId, reminderName, props);
            bot.sendMessage(chatId, "Повторяющееся напоминание \"" + reminderName + "\" добавлено на " + daysPart + " в " + timePart + ".");
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("уже существует")) {
                bot.sendMessage(chatId, "Напоминание с именем \"" + reminderName + "\" уже существует. Пожалуйста, выберите другое имя или введите /cancel.");
            } else {
                e.printStackTrace();
                bot.sendMessage(chatId, "Ошибка при добавлении напоминания. Попробуйте позже.");
            }
        } finally {
            fsm.resetState(userId);
        }
    }
	
    private List<String> parseDays(String input) {
        List<String> result = new ArrayList<>();
        
        // Заменяем все виды тире на обычный дефис
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
                    return new ArrayList<>(); // Ошибка парсинга
                }
                result.addAll(daysInRange);
            } else {
                DayOfWeek dow = resolveDayOfWeek(rangeOrItem);
                if (dow == null) {
                    return new ArrayList<>(); // Нераспознанный день
                }
                result.add(dow.toString().substring(0, 3));
            }
        }

        // Убираем дубликаты, сохраняя порядок
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
        } catch (IllegalArgumentException ignored) {}

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
}
