package bot.fsm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BotFSM {

    // Единственный экземпляр — Singleton
    public static final BotFSM INSTANCE = new BotFSM();
    
    public static BotFSM getInstance() {
        return INSTANCE;
    }

    private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();

    // Временное хранилище данных
    private final Map<Long, Map<String, Object>> tempData = new ConcurrentHashMap<>();
    
    // Приватный конструктор
    private BotFSM() {}
    
    // Метод для установки временных данных
    public void setTempData(Long userId, String key, Object value) {
        tempData.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(key, value);
    }
    
    // Метод для получения временных данных
    public Object getTempData(Long userId, String key) {
        Map<String, Object> userData = tempData.get(userId);
        if (userData != null) {
            return userData.get(key);
        }
        return null;
    }
    
    // Метод для удаления временных данных
    public void clearTempData(Long userId, String key) {
        Map<String, Object> userData = tempData.get(userId);
        if (userData != null) {
            userData.remove(key);
        }
    }

    // Метод для полной очистки временных данных
    public void clearAllTempData(Long userId) {
        tempData.remove(userId);
    }

    public UserState getState(Long userId) {
        return userStates.getOrDefault(userId, UserState.DEFAULT);
    }

    public void setState(Long userId, UserState state) {
        userStates.put(userId, state);
    }

    public void resetState(Long userId) {
        userStates.remove(userId);
        tempData.remove(userId);
    }
}