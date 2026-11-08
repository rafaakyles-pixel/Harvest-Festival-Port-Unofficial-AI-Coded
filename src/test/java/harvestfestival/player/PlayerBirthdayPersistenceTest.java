package harvestfestival.player;

import harvestfestival.calendar.BirthdayDate;
import harvestfestival.calendar.Season;
import io.netty.buffer.Unpooled;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.attachment.AttachmentInternals;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PlayerBirthdayPersistenceTest {
    @TempDir Path directory;

    @Test
    void absentByDefaultAndAfterLoadingMissingData() {
        var holder = new SavedHolder();
        assertTrue(holder.getData(PlayerBirthday.BIRTHDAY).isEmpty());
        holder = new SavedHolder();
        holder.load(new CompoundTag());
        assertTrue(holder.getData(PlayerBirthday.BIRTHDAY).isEmpty());
    }

    @Test
    void setAndClearSurviveCompressedNbt() throws Exception {
        var holder = new SavedHolder();
        assertTrue(reopen(holder).getData(PlayerBirthday.BIRTHDAY).isEmpty());
        var date = new BirthdayDate(Season.WINTER, 30);
        PlayerBirthday.setOn(holder, date);
        holder = reopen(holder);
        assertEquals(Optional.of(date), holder.getData(PlayerBirthday.BIRTHDAY));
        PlayerBirthday.clearOn(holder);
        assertTrue(reopen(holder).getData(PlayerBirthday.BIRTHDAY).isEmpty());
    }

    @Test
    void holdersRemainIndependent() {
        var first = new SavedHolder();
        var second = new SavedHolder();
        var date = new BirthdayDate(Season.SUMMER, 1);
        PlayerBirthday.setOn(first, date);
        assertTrue(second.getData(PlayerBirthday.BIRTHDAY).isEmpty());
        PlayerBirthday.setOn(second, new BirthdayDate(Season.WINTER, 30));
        PlayerBirthday.clearOn(second);
        assertEquals(Optional.of(date), first.getData(PlayerBirthday.BIRTHDAY));
    }

    @Test
    void deathCopyUsesRegisteredFlagAndNeoForgeSerialization() throws Exception {
        // Exercise NeoForge's actual copy routine without constructing a server/world.
        // Reflection is confined to this test because the holder-level routine is private.
        var flag = AttachmentType.class.getDeclaredField("copyOnDeath");
        flag.setAccessible(true);
        assertTrue(flag.getBoolean(PlayerBirthday.BIRTHDAY.get()));
        var method = AttachmentInternals.class.getDeclaredMethod("copyAttachments", HolderLookup.Provider.class,
                AttachmentHolder.class, AttachmentHolder.class, Predicate.class);
        method.setAccessible(true);
        Predicate<AttachmentType<?>> deathFilter = type -> {
            try { return flag.getBoolean(type); }
            catch (IllegalAccessException ex) { throw new AssertionError(ex); }
        };
        var original = new SavedHolder();
        for (var value : java.util.List.of(Optional.of(new BirthdayDate(Season.AUTUMN, 20)), Optional.<BirthdayDate>empty())) {
            original.setData(PlayerBirthday.BIRTHDAY, value);
            var replacement = new SavedHolder();
            method.invoke(null, HolderLookup.Provider.create(Stream.empty()), original, replacement, deathFilter);
            assertEquals(value, replacement.getData(PlayerBirthday.BIRTHDAY));
        }
    }

    @Test
    void networkCodecRoundTripsDateAndClear() {
        var buffer = Unpooled.buffer();
        try {
            for (var value : java.util.List.of(Optional.of(new BirthdayDate(Season.SPRING, 1)),
                    Optional.of(new BirthdayDate(Season.WINTER, 30)), Optional.<BirthdayDate>empty())) {
                PlayerBirthday.STREAM_CODEC.encode(buffer, value);
                assertEquals(value, PlayerBirthday.STREAM_CODEC.decode(buffer));
                assertFalse(buffer.isReadable());
            }
        } finally { buffer.release(); }
    }

    private SavedHolder reopen(SavedHolder holder) throws Exception {
        var file = directory.resolve("birthday.dat");
        NbtIo.writeCompressed(holder.save(), file);
        var loaded = new SavedHolder();
        loaded.load(NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()));
        return loaded;
    }

    private static final class SavedHolder extends AttachmentHolder {
        CompoundTag save() {
            var output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
            serializeAttachments(output);
            return output.buildResult();
        }
        void load(CompoundTag tag) {
            deserializeAttachments(TagValueInput.create(ProblemReporter.DISCARDING,
                    HolderLookup.Provider.create(Stream.empty()), tag));
        }
    }
}
