package bot.fsm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BotFSM {

    // Единственный экземпляр — Singleton
    public static final BotFSM INSTANCE = new BotFSM();

    private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();

    // Приватный конструктор
    private BotFSM() {}

    public UserState getState(Long userId) {
        return userStates.getOrDefault(userId, UserState.DEFAULT);
    }

    public void setState(Long userId, UserState state) {
        userStates.put(userId, state);
    }

    public void resetState(Long userId) {
        userStates.remove(userId);
    }
}