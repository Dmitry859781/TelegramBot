package bot.timezone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class UserTimezoneServiceTest {

    @TempDir
    Path tempDir;

    private UserTimezoneService testInstance;

    @BeforeEach
    void setUp() throws Exception {
        // Безопасный путь к временной БД 
        String dbPath = tempDir.resolve("test-timezone.db").toAbsolutePath().toString().replace('\\', '/');
        String dbUrl = "jdbc:sqlite:" + dbPath;

        Constructor<UserTimezoneService> constructor = UserTimezoneService.class.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        
        // Тестовый экземпляр
        testInstance = constructor.newInstance(dbUrl);
    }

    @Test
    @DisplayName("Тест сохранения и получения часового пояса")
    void testSaveAndGetTimezone() throws SQLException {
        Long userId = 1L;
        int expectedOffset = 3;

        testInstance.saveTimezoneOffset(userId, expectedOffset);
        Integer retrievedOffset = testInstance.getTimezone(userId);

        assertEquals(expectedOffset, retrievedOffset, "Часовой пояс должен совпадать с сохраненным");
    }

    @Test
    @DisplayName("Тест обновления (замены) существующего часового пояса")
    void testUpdateTimezone() throws SQLException {
        Long userId = 1L;

        // Сначала сохраняем одно значение
        testInstance.saveTimezoneOffset(userId, 3);
        assertEquals(3, testInstance.getTimezone(userId));

        // Обновляем на другое значение
        int newOffset = -5;
        testInstance.saveTimezoneOffset(userId, newOffset);
        
        assertEquals(newOffset, testInstance.getTimezone(userId), "Часовой пояс должен быть обновлен");
    }

    @Test
    @DisplayName("Тест получения часового пояса для несуществующего пользователя")
    void testGetNonExistentUserTimezone() throws SQLException {
        Long userId = 999L; // Пользователь, которого нет в базе

        Integer retrievedOffset = testInstance.getTimezone(userId);
        
        assertNull(retrievedOffset, "Для несуществующего пользователя должен возвращаться null");
    }

    @Test
    @DisplayName("Тест сохранения нулевого смещения (UTC+0)")
    void testSaveZeroTimezone() throws SQLException {
        Long userId = 1L;
        int expectedOffset = 0;

        testInstance.saveTimezoneOffset(userId, expectedOffset);
        Integer retrievedOffset = testInstance.getTimezone(userId);

        assertEquals(expectedOffset, retrievedOffset, "Часовой пояс UTC+0 должен сохраняться корректно");
    }
}