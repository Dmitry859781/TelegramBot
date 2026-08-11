// src/test/java/bot/note/NoteServiceTest.java
package bot.note;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NoteServiceTest {

    @TempDir
    File tempDir;

    private NoteService noteService;
    private String testDbPath;

    @BeforeEach
    void setUp() throws SQLException {
        // Создаём путь к временной базе в поддиректории database_for_test
        Path testDbDir = tempDir.toPath().resolve("database_for_test");
        try {
            Files.createDirectory(testDbDir);
        } catch (Exception e) {
            // Игнорируем, если уже существует
        }
        Path testDbFile = testDbDir.resolve("test-notes.db");
        testDbPath = testDbFile.toString();

        // Используем вспомогательный конструктор для тестов
        noteService = new NoteService(testDbPath, true);
    }

    @AfterEach
    void tearDown() {
        // Удалить файл базы данных после теста
        new File(testDbPath).delete();
    }

    @Test
    @DisplayName("Тест добавления и получения заметки")
    void testAddAndGetNote() throws SQLException {
        Long userId = 1L;
        String noteName = "test_note";
        String text = "Это тестовая заметка.";

        noteService.addNoteToDB(userId, noteName, text);

        String retrievedText = noteService.getNote(userId, noteName);
        assertEquals(text, retrievedText, "Текст заметки должен совпадать с добавленным");
    }

    @Test
    @DisplayName("Тест получения несуществующей заметки")
    void testGetNonExistentNoteReturnsNull() throws SQLException {
        Long userId = 1L;
        String noteName = "nonexistent";

        String retrievedText = noteService.getNote(userId, noteName);
        assertNull(retrievedText, "Для несуществующей заметки должен возвращаться null");
    }

    @Test
    @DisplayName("Тест получения списка заметок пользователя")
    void testGetUserNotes() throws SQLException {
        Long userId = 1L;
        noteService.addNoteToDB(userId, "note1", "Текст 1");
        noteService.addNoteToDB(userId, "note2", "Текст 2");

        List<String> userNotes = noteService.getUserNotes(userId);
        assertEquals(2, userNotes.size(), "Пользователь должен иметь 2 заметки");
        assertTrue(userNotes.contains("note1"), "Список должен содержать 'note1'");
        assertTrue(userNotes.contains("note2"), "Список должен содержать 'note2'");
    }

    @Test
    @DisplayName("Тест получения пустого списка заметок")
    void testGetUserNotesEmptyList() throws SQLException {
        Long userId = 1L;

        List<String> userNotes = noteService.getUserNotes(userId);
        assertTrue(userNotes.isEmpty(), "Список заметок должен быть пустым для нового пользователя");
    }

    @Test
    @DisplayName("Тест удаления заметки")
    void testRemoveNote() throws SQLException {
        Long userId = 1L;
        String noteName = "to_be_deleted";
        String text = "Эта заметка будет удалена.";
        noteService.addNoteToDB(userId, noteName, text);

        // Удаляем
        noteService.removeNoteFromDB(userId, noteName);

        // Проверяем, что больше не существует
        String retrievedText = noteService.getNote(userId, noteName);
        assertNull(retrievedText, "После удаления заметка не должна быть найдена");
    }

    @Test
    @DisplayName("Тест обновления заметки")
    void testUpdateNote() throws SQLException {
        Long userId = 1L;
        String noteName = "update_test";
        String initialText = "Старый текст";
        String updatedText = "Новый текст";

        noteService.addNoteToDB(userId, noteName, initialText);
        assertEquals(initialText, noteService.getNote(userId, noteName));

        // Обновляем
        noteService.addNoteToDB(userId, noteName, updatedText);

        assertEquals(updatedText, noteService.getNote(userId, noteName), "Текст заметки должен быть обновлён");
    }
}