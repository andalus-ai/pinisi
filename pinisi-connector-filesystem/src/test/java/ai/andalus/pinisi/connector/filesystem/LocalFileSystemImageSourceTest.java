package ai.andalus.pinisi.connector.filesystem;
import ai.andalus.pinisi.api.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class LocalFileSystemImageSourceTest {
    @TempDir Path temp;
    @Test void discoversSupportedImagesAndOpensThem() throws Exception {
        Path nested = Files.createDirectories(temp.resolve("nested"));
        Files.write(nested.resolve("photo.JPG"), new byte[]{1,2,3});
        Files.writeString(temp.resolve("notes.txt"), "not an image");
        var source = new LocalFileSystemImageSource("photos", temp);
        var assets = source.discover();
        assertEquals(1, assets.size());
        assertEquals(MediaType.IMAGE, assets.getFirst().mediaType());
        assertEquals("nested/photo.JPG", assets.getFirst().sourceKey());
        try (var in = source.open(assets.getFirst())) { assertArrayEquals(new byte[]{1,2,3}, in.readAllBytes()); }
        assertEquals(assets.getFirst().id(), source.discover().getFirst().id());
    }
    @Test void rejectsAssetsFromAnotherSource() throws Exception {
        var source = new LocalFileSystemImageSource("photos", temp);
        var other = new ai.andalus.pinisi.api.Asset("x","default","other","a.png",MediaType.IMAGE,
                temp.resolve("a.png").toUri(),java.util.Map.of());
        assertThrows(IllegalArgumentException.class, () -> source.open(other));
    }
}