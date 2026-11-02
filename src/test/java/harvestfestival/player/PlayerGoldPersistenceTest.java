package harvestfestival.player;

import java.nio.file.Path;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerGoldPersistenceTest {
    @TempDir
    Path directory;

    @Test
    void newHoldersStartAtZeroAndBalancesAreIndependent() {
        var first = new SavedHolder();
        var second = new SavedHolder();
        assertEquals(0L, first.getData(PlayerGold.GOLD));
        assertEquals(0L, second.getData(PlayerGold.GOLD));
        first.setData(PlayerGold.GOLD, 125L);
        second.setData(PlayerGold.GOLD, -50L);
        assertEquals(125L, first.getData(PlayerGold.GOLD));
        assertEquals(-50L, second.getData(PlayerGold.GOLD));
    }

    @Test
    void registeredAttachmentSurvivesCompressedSaveAndFreshLoad() throws Exception {
        long[] amounts = {0L, 125L, -50L, 2147483648L, Long.MIN_VALUE, Long.MAX_VALUE};
        for (int index = 0; index < amounts.length; index++) {
            var original = new SavedHolder();
            original.setData(PlayerGold.GOLD, amounts[index]);
            var file = directory.resolve(index + ".dat");
            NbtIo.writeCompressed(original.save(), file);

            var reloaded = new SavedHolder();
            reloaded.load(NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()));
            assertEquals(amounts[index], reloaded.getData(PlayerGold.GOLD));
        }
    }

    @Test
    void saveWithoutGoldLoadsWithZeroBalance() {
        var reloaded = new SavedHolder();
        reloaded.load(new CompoundTag());
        assertEquals(0L, reloaded.getData(PlayerGold.GOLD));
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
