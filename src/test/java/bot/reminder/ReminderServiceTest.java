package bot.reminder;

import bot.reminder.once.OnceProperties;
import bot.reminder.recurring.RecurringProperties;
import bot.reminder.recurring.ScheduleItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReminderServiceTest {

    @TempDir
    Path tempDir;

    private ReminderService testInstance;

    @BeforeEach
    void setUp() throws Exception {
        // Безопасный путь к временной БД
        String dbPath = tempDir.resolve("test-reminders.db").toAbsolutePath().toString().replace('\\', '/');
        String dbUrl = "jdbc:sqlite:" + dbPath;

        Constructor<ReminderService> constructor = ReminderService.class.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        
        // Тестовый экземпляр
        testInstance = constructor.newInstance(dbUrl);
    }

    @Test
    @DisplayName("Тест добавления и получения разового напоминания")
    void testAddAndGetOnceReminder() throws SQLException {
        Long userId = 1L;
        String name = "test_once";
        
        // 15:30 по UTC+3. В базу должно сохраниться как 12:30 UTC
        LocalDateTime remindAt = LocalDateTime.of(2026, 8, 27, 15, 30);
        ZoneId userZone = ZoneOffset.ofHours(3);

        testInstance.addOnceReminder(userId, name, remindAt, userZone);

        Reminder retrieved = testInstance.getReminder(userId, name);
        
        assertNotNull(retrieved, "Напоминание должно быть найдено");
        assertEquals(name, retrieved.getName());
        assertEquals(ReminderType.ONCE, retrieved.getType());
        
        OnceProperties props = retrieved.getPropertiesAs(OnceProperties.class);
        // Проверяем, что время корректно сконвертировано в UTC и отформатировано
        assertEquals("2026-08-27 12:30:00", props.remind_at);
    }

    @Test
    @DisplayName("Тест добавления и получения повторяющегося напоминания")
    void testAddAndGetRecurringReminder() throws SQLException {
        Long userId = 1L;
        String name = "test_recurring";
        
        RecurringProperties props = new RecurringProperties();
        ScheduleItem item = new ScheduleItem();
        item.day = "MON";
        item.time = "09:00";
        props.schedule = List.of(item);

        testInstance.addRecurringReminder(userId, name, props);

        Reminder retrieved = testInstance.getReminder(userId, name);
        
        assertNotNull(retrieved);
        assertEquals(ReminderType.RECURRING, retrieved.getType());
        
        RecurringProperties retrievedProps = retrieved.getPropertiesAs(RecurringProperties.class);
        assertEquals(1, retrievedProps.schedule.size());
        assertEquals("MON", retrievedProps.schedule.get(0).day);
        assertEquals("09:00", retrievedProps.schedule.get(0).time);
    }

    @Test
    @DisplayName("Тест получения ВСЕХ имен напоминаний (type = null)")
    void testGetUserReminderNames_All() throws SQLException {
        Long userId = 1L;
        testInstance.addOnceReminder(userId, "z_reminder", LocalDateTime.now(), ZoneId.systemDefault());
        testInstance.addOnceReminder(userId, "a_reminder", LocalDateTime.now(), ZoneId.systemDefault());

        // Передаем null, чтобы получить все напоминания без фильтрации
        List<String> names = testInstance.getUserReminderNames(userId, null);
        
        assertEquals(2, names.size(), "Должно быть 2 напоминания");
        // Проверяем сортировку по алфавиту (ORDER BY reminder_name в SQL)
        assertEquals("a_reminder", names.get(0));
        assertEquals("z_reminder", names.get(1));
    }

    @Test
    @DisplayName("Тест фильтрации имен напоминаний по типу (ONCE и RECURRING)")
    void testGetUserReminderNames_FilteredByType() throws SQLException {
        Long userId = 1L;

        // Добавляем 2 разовых и 1 повторяющееся напоминание
        testInstance.addOnceReminder(userId, "once_1", LocalDateTime.now().plusDays(1), ZoneId.systemDefault());
        testInstance.addOnceReminder(userId, "once_2", LocalDateTime.now().plusDays(2), ZoneId.systemDefault());

        RecurringProperties props = new RecurringProperties();
        ScheduleItem item = new ScheduleItem();
        item.day = "MON";
        item.time = "09:00";
        props.schedule = List.of(item);
        testInstance.addRecurringReminder(userId, "recurring_1", props);

        // Проверяем получение только ONCE
        List<String> onceNames = testInstance.getUserReminderNames(userId, "ONCE");
        assertEquals(2, onceNames.size(), "Должно быть 2 разовых напоминания");
        assertTrue(onceNames.contains("once_1"));
        assertTrue(onceNames.contains("once_2"));
        assertFalse(onceNames.contains("recurring_1"));

        // Проверяем получение только RECURRING
        List<String> recurringNames = testInstance.getUserReminderNames(userId, "RECURRING");
        assertEquals(1, recurringNames.size(), "Должно быть 1 повторяющееся напоминание");
        assertEquals("recurring_1", recurringNames.get(0));

        // Проверяем получение ВСЕХ
        List<String> allNames = testInstance.getUserReminderNames(userId, null);
        assertEquals(3, allNames.size(), "Должно быть 3 напоминания всего");
    }

    @Test
    @DisplayName("Тест удаления напоминания")
    void testRemoveReminder() throws SQLException {
        Long userId = 1L;
        String name = "to_be_deleted";
        
        testInstance.addOnceReminder(userId, name, LocalDateTime.now(), ZoneId.systemDefault());
        assertNotNull(testInstance.getReminder(userId, name));

        testInstance.removeReminder(userId, name);

        Reminder retrieved = testInstance.getReminder(userId, name);
        assertNull(retrieved, "После удаления напоминание не должно быть найдено");
    }

    @Test
    @DisplayName("Тест обновления напоминания")
    void testUpdateReminder() throws SQLException {
        Long userId = 1L;
        String oldName = "old_name";
        String newName = "new_name";
        
        testInstance.addOnceReminder(userId, oldName, LocalDateTime.now(), ZoneId.systemDefault());

        OnceProperties newProps = new OnceProperties();
        newProps.remind_at = "2027-01-01 12:00:00";
        String jsonProps = new com.google.gson.Gson().toJson(newProps);

        testInstance.updateReminder(userId, oldName, newName, jsonProps);

        // Проверяем, что новое имя существует и свойства обновлены
        Reminder retrievedNew = testInstance.getReminder(userId, newName);
        assertNotNull(retrievedNew);
        assertEquals(newName, retrievedNew.getName());
        assertEquals("2027-01-01 12:00:00", retrievedNew.getPropertiesAs(OnceProperties.class).remind_at);

        // Проверяем, что старое имя больше не существует
        Reminder retrievedOld = testInstance.getReminder(userId, oldName);
        assertNull(retrievedOld, "Старое имя должно быть заменено");
    }

    @Test
    @DisplayName("Тест getDueReminders: срабатывание разового напоминания в прошлом")
    void testGetDueReminders_Once() throws SQLException {
        Long userId = 1L;
        
        // Создаем напоминание на 10 минут в прошлом относительно системного времени
        LocalDateTime pastTime = LocalDateTime.now(ZoneId.systemDefault()).minusMinutes(10);
        testInstance.addOnceReminder(userId, "due_once", pastTime, ZoneId.systemDefault());

        List<Reminder> due = testInstance.getDueReminders();
        
        assertEquals(1, due.size(), "Должно быть найдено 1 просроченное напоминание");
        assertEquals("due_once", due.get(0).getName());
    }

    @Test
    @DisplayName("Тест getDueReminders: срабатывание повторяющегося напоминания")
    void testGetDueReminders_Recurring() throws SQLException {
        Long userId = 1L;
        
        String currentDay = LocalDateTime.now(ZoneId.systemDefault()).getDayOfWeek().toString();
        String currentTime = LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"));

        RecurringProperties props = new RecurringProperties();
        ScheduleItem item = new ScheduleItem();
        item.day = currentDay.substring(0, 3); 
        item.time = currentTime;
        props.schedule = List.of(item);

        testInstance.addRecurringReminder(userId, "due_recurring", props);

        List<Reminder> due = testInstance.getDueReminders();
        
        assertEquals(1, due.size(), "Должно быть найдено 1 повторяющееся напоминание");
        assertEquals("due_recurring", due.get(0).getName());
    }
}