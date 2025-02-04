package dev.darealturtywurty.superturtybot.database.codecs;

import com.lukaspradel.steamapi.data.json.appnews.Newsitem;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class NewsitemCodec implements Codec<Newsitem> {
    @Override
    public Newsitem decode(BsonReader reader, DecoderContext decoderContext) {
        reader.readStartDocument();
        Newsitem newsitem = new Newsitem()
                .withAuthor(reader.readString("author"))
                .withContents(reader.readString("contents"))
                .withDate(reader.readInt64("date"))
                .withFeedlabel(reader.readString("feedlabel"))
                .withFeedname(reader.readString("feedname"))
                .withGid(reader.readString("gid"))
                .withIsExternalUrl(reader.readBoolean("is_external_url"))
                .withTitle(reader.readString("title"))
                .withUrl(reader.readString("url"));
        reader.readEndDocument();
        return newsitem;
    }

    @Override
    public void encode(BsonWriter writer, Newsitem value, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeString("author", value.getAuthor());
        writer.writeString("contents", value.getContents());
        writer.writeInt64("date", value.getDate());
        writer.writeString("feedlabel", value.getFeedlabel());
        writer.writeString("feedname", value.getFeedname());
        writer.writeString("gid", value.getGid());
        writer.writeBoolean("is_external_url", value.getIsExternalUrl());
        writer.writeString("title", value.getTitle());
        writer.writeString("url", value.getUrl());
        writer.writeEndDocument();
    }

    @Override
    public Class<Newsitem> getEncoderClass() {
        return Newsitem.class;
    }
}
