package dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.mcp;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.MappingDownloads;
import dev.darealturtywurty.superturtybot.commands.util.minecraft.mappings.MappingFiles;

import javax.xml.stream.XMLInputFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public final class ForgeMaven {
    private static final String BASE_URL = "https://maven.minecraftforge.net/de/oceanlabs/mcp/";
    private static final Cache<String, List<String>> VERSIONS = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    private ForgeMaven() {
    }

    public static List<String> versions(String artifact) throws IOException {
        try {
            return VERSIONS.get(artifact, () -> {
                byte[] xml = MappingDownloads.download(BASE_URL + artifact + "/maven-metadata.xml");
                var factory = XMLInputFactory.newFactory();
                factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
                factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
                var reader = factory.createXMLStreamReader(new ByteArrayInputStream(xml));
                try {
                    var versions = new ArrayList<String>();
                    while (reader.hasNext()) {
                        reader.next();
                        if (reader.isStartElement() && reader.getLocalName().equals("version")) {
                            versions.add(reader.getElementText());
                        }
                    }

                    return List.copyOf(versions);
                } finally {
                    reader.close();
                }
            });
        } catch (ExecutionException exception) {
            throw new IOException("Unable to load Forge mapping versions", exception.getCause());
        }
    }

    public static Path download(String artifact, String build, String classifier, Path directory) throws IOException {
        String filename = artifact + "-" + build + classifier + ".zip";
        return MappingDownloads.cached(BASE_URL + artifact + "/" + MappingFiles.encode(build)
                + "/" + MappingFiles.encode(filename), directory.resolve(filename), null);
    }
}
