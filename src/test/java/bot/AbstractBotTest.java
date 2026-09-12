package bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import bot.TelegramBot;
import bot.fsm.BotFSM;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public abstract class AbstractBotTest {

    @Mock
    protected TelegramBot bot;
    @Mock
    protected Message message;
    @Mock
    protected BotFSM fsm;
    @Mock
    protected User mockUser;

    protected final Long userId = 12345L;
    protected final Long chatId = 67890L;

    @BeforeEach
    void setUpBase() {
        when(mockUser.getId()).thenReturn(userId);
        when(message.getFrom()).thenReturn(mockUser);
        when(message.getChatId()).thenReturn(chatId);
    }
}