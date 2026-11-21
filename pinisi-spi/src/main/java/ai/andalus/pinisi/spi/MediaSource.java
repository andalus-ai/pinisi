package ai.andalus.pinisi.spi;
import ai.andalus.pinisi.api.Asset;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
public interface MediaSource {
    String id();
    List<Asset> discover() throws IOException;
    InputStream open(Asset asset) throws IOException;
}