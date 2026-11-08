package dev.darealturtywurty.superturtybot.database.codecs;

import com.lukaspradel.steamapi.data.json.appnews.Newsitem;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class NewsitemCodec implements Codec<Newsitem> {
    @Override
    public Newsitem decode(BsonReader reader, DecoderContext decoderContext) {
        var newsitem = new Newsitem();
        boolean hasExternalUrl = false;
        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            switch (reader.readName()) {
                case "author" -> newsitem.setAuthor(readString(reader));
                case "contents" -> newsitem.setContents(readString(reader));
                case "date" -> newsitem.setDate(readDate(reader));
                case "feedlabel" -> newsitem.setFeedlabel(readString(reader));
                case "feedname" -> newsitem.setFeedname(readString(reader));
                case "gid" -> newsitem.setGid(readString(reader));
                case "is_external_url" -> {
                    newsitem.setIsExternalUrl(readBoolean(reader));
                    hasExternalUrl = true;
                }
                case "isExternalUrl", "externalUrl" -> {
                    Boolean externalUrl = readBoolean(reader);
                    if (!hasExternalUrl) {
                        newsitem.setIsExternalUrl(externalUrl);
                    }
                }
                case "title" -> newsitem.setTitle(readString(reader));
                case "url" -> newsitem.setUrl(readString(reader));
                default -> reader.skipValue();
            }
        }
        reader.readEndDocument();
        return newsitem;
    }

    @Override
    public void encode(BsonWriter writer, Newsitem value, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writeString(writer, "author", value.getAuthor());
        writeString(writer, "contents", value.getContents());
        if (value.getDate() == null) {
            writer.writeNull("date");
        } else {
            writer.writeInt64("date", value.getDate());
        }
        writeString(writer, "feedlabel", value.getFeedlabel());
        writeString(writer, "feedname", value.getFeedname());
        writeString(writer, "gid", value.getGid());
        if (value.getIsExternalUrl() == null) {
            writer.writeNull("is_external_url");
        } else {
            writer.writeBoolean("is_external_url", value.getIsExternalUrl());
        }
        writeString(writer, "title", value.getTitle());
        writeString(writer, "url", value.getUrl());
        writer.writeEndDocument();
    }

    @Override
    public Class<Newsitem> getEncoderClass() {
        return Newsitem.class;
    }

    private static Long readDate(BsonReader reader) {
        return switch (reader.getCurrentBsonType()) {
            case INT32 -> (long) reader.readInt32();
            case NULL -> {
                reader.readNull();
                yield null;
            }
            default -> reader.readInt64();
        };
    }

    private static String readString(BsonReader reader) {
        if (reader.getCurrentBsonType() == BsonType.NULL) {
            reader.readNull();
            return null;
        }
        return reader.readString();
    }

    private static Boolean readBoolean(BsonReader reader) {
        if (reader.getCurrentBsonType() == BsonType.NULL) {
            reader.readNull();
            return null;
        }
        return reader.readBoolean();
    }

    private static void writeString(BsonWriter writer, String name, String value) {
        if (value == null) {
            writer.writeNull(name);
        } else {
            writer.writeString(name, value);
        }
    }
}
