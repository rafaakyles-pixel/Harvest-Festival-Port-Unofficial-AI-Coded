package harvestfestival.calendar.command;

import io.netty.buffer.Unpooled;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NpcBirthdayCommandsTest {
    @Test
    void booleanFeedbackRemainsTextAfterNetworkSerialization() {
        var buffer = Unpooled.buffer();
        try {
            for (boolean matches : new boolean[]{true, false}) {
                var message = NpcBirthdayCommands.testResult(Identifier.parse("harvestfestival:npc/test_example"), matches);
                ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(buffer, message);
                var received = ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(buffer);
                var contents = assertInstanceOf(TranslatableContents.class, received.getContents());
                assertEquals(Boolean.toString(matches), contents.getArgs()[1]);
                assertFalse(buffer.isReadable());
            }
        } finally { buffer.release(); }
    }
}
