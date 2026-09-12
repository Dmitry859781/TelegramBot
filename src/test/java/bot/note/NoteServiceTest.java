package bot.note;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NoteServiceTest {

    @TempDir
    Path tempDir;

    private NoteService testInstance;

    @BeforeEach
    void setUp() throws Exception {
    	// Безопасный путь к временной БД
        String dbPath = tempDir.resolve("test-notes.db").toAbsolutePath().toString().replace('\\', '/');

        Constructor<NoteService> constructor = NoteService.class.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        
        // Тестовый экземпляр
        testInstance = constructor.newInstance(dbPath);
    }

    @Test
    @DisplayName("Тест добавления и получения заметки")
    void testAddAndGetNote() throws SQLException {
        Long userId = 1L;
        String noteName = "test_note";
        String text = "Это тестовая заметка.";

        testInstance.addNoteToDB(userId, noteName, text);
        String retrievedText = testInstance.getNote(userId, noteName);

        assertEquals(text, retrievedText, "Текст заметки должен совпадать с добавленным");
    }

    @Test
    @DisplayName("Тест получения несуществующей заметки")
    void testGetNonExistentNoteReturnsNull() throws SQLException {
        Long userId = 1L;
        String noteName = "nonexistent";

        String retrievedText = testInstance.getNote(userId, noteName);
        assertNull(retrievedText, "Для несуществующей заметки должен возвращаться null");
    }

    @Test
    @DisplayName("Тест получения списка заметок пользователя")
    void testGetUserNotes() throws SQLException {
        Long userId = 1L;
        testInstance.addNoteToDB(userId, "note1", "Текст 1");
        testInstance.addNoteToDB(userId, "note2", "Текст 2");

        List<String> userNotes = testInstance.getUserNotes(userId);
        
        assertEquals(2, userNotes.size(), "Пользователь должен иметь 2 заметки");
        assertTrue(userNotes.contains("note1"), "Список должен содержать 'note1'");
        assertTrue(userNotes.contains("note2"), "Список должен содержать 'note2'");
    }

    @Test
    @DisplayName("Тест получения пустого списка заметок")
    void testGetUserNotesEmptyList() throws SQLException {
        Long userId = 999L;

        List<String> userNotes = testInstance.getUserNotes(userId);
        assertTrue(userNotes.isEmpty(), "Список заметок должен быть пустым для нового пользователя");
    }

    @Test
    @DisplayName("Тест удаления заметки")
    void testRemoveNote() throws SQLException {
        Long userId = 1L;
        String noteName = "to_be_deleted";
        
        testInstance.addNoteToDB(userId, noteName, "Эта заметка будет удалена.");
        assertNotNull(testInstance.getNote(userId, noteName));

        testInstance.removeNoteFromDB(userId, noteName);

        String retrievedText = testInstance.getNote(userId, noteName);
        assertNull(retrievedText, "После удаления заметка не должна быть найдена");
    }

    @Test
    @DisplayName("Тест обновления (перезаписи) заметки")
    void testUpdateNote() throws SQLException {
        Long userId = 1L;
        String noteName = "update_test";
        String initialText = "Старый текст";
        String updatedText = "Новый текст";

        testInstance.addNoteToDB(userId, noteName, initialText);
        assertEquals(initialText, testInstance.getNote(userId, noteName));

        testInstance.addNoteToDB(userId, noteName, updatedText);

        assertEquals(updatedText, testInstance.getNote(userId, noteName), "Текст заметки должен быть обновлён");
    }
}