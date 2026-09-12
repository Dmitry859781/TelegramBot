package bot.note;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.*;

public class NoteService {

    // Единственный экземпляр — Singleton
    public static final NoteService INSTANCE = new NoteService("database/Notes.db");

    public static NoteService getInstance() {
        return INSTANCE;
    }
    
    private final String DB_URL;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }
    }
    
    // Конструктор
    private NoteService(String dbPath) {
        Path dbFilePath = Paths.get(dbPath);
        Path dbDir = dbFilePath.getParent();
        if (dbDir != null && !Files.exists(dbDir)) {
            try {
                Files.createDirectories(dbDir);
            } catch (java.io.IOException e) {
                throw new RuntimeException("Failed to create database directory: " + dbDir, e);
            }
        }

        this.DB_URL = "jdbc:sqlite:" + dbPath;
        initializeDatabase();
    }

    // Создаёт таблицу при первом запуске
    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // System.out.println("Creating table...");
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS notes (
                    user_id INTEGER NOT NULL,
                    note_name TEXT NOT NULL,
                    text TEXT NOT NULL,
                    PRIMARY KEY (user_id, note_name)
                )
                """);
            // System.out.println("Table created!");
        } catch (SQLException e) {
            System.err.println("Error initializing database:");
            throw new RuntimeException("Error initializing NoteService", e);
        }
    }

    // Добавление заметки
    public void addNoteToDB(Long userId, String noteName, String text) throws SQLException {
        String sql = "INSERT OR REPLACE INTO notes (user_id, note_name, text) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, noteName);
            stmt.setString(3, text);
            stmt.executeUpdate();
        }
    }
    
    // Обновление заметки
    public void updateNote(Long userId, String oldName, String newName, String newText) throws SQLException {
        // Сначала переименовываем
        if (!oldName.equals(newName)) {
            String renameSql = "UPDATE notes SET note_name = ? WHERE user_id = ? AND note_name = ?";
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement stmt = conn.prepareStatement(renameSql)) {
                stmt.setString(1, newName);
                stmt.setLong(2, userId);
                stmt.setString(3, oldName);
                stmt.executeUpdate();
            }
        }
        // Затем обновляем текст
        String updateTextSql = "UPDATE notes SET text = ? WHERE user_id = ? AND note_name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(updateTextSql)) {
            stmt.setString(1, newText);
            stmt.setLong(2, userId);
            stmt.setString(3, newName);
            stmt.executeUpdate();
        }
    }

    // Удаление заметки по userId и noteName
    public void removeNoteFromDB(Long userId, String noteName) throws SQLException {
        String sql = "DELETE FROM notes WHERE user_id = ? AND note_name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, noteName);
            stmt.executeUpdate();
        }
    }

    // Возвращает текст заметки по userId и noteName
    public String getNote(Long userId, String noteName) throws SQLException {
        String sql = "SELECT text FROM notes WHERE user_id = ? AND note_name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, noteName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("text");
                }
                return null; // Заметка не найдена
            }
        }
    }

    // Получение списка имён заметок пользователя
    public List<String> getUserNotes(Long userId) throws SQLException {
        String sql = "SELECT note_name FROM notes WHERE user_id = ? ORDER BY note_name";
        List<String> noteNames = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    noteNames.add(rs.getString("note_name"));
                }
            }
        }
        return noteNames;
    }
}