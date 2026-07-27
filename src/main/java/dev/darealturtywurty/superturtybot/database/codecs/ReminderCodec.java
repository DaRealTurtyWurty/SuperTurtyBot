package dev.darealturtywurty.superturtybot.database.codecs;

import dev.darealturtywurty.superturtybot.database.pojos.collections.Reminder;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class ReminderCodec implements Codec<Reminder> {
    @Override
    public Reminder decode(BsonReader reader, DecoderContext decoderContext) {
        var reminder = new Reminder();

        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            switch (reader.readName()) {
                case "_id" -> reminder.setId(readId(reader));
                case "guild" -> reminder.setGuild(readLong(reader));
                case "user" -> reminder.setUser(readLong(reader));
                case "reminder" -> reminder.setReminder(readString(reader));
                case "channel" -> reminder.setChannel(readLong(reader));
                case "time" -> reminder.setTime(readLong(reader));
                case "createdAt" -> reminder.setCreatedAt(readLong(reader));
                default -> reader.skipValue();
            }
        }
        reader.readEndDocument();

        return reminder;
    }

    @Override
    public void encode(BsonWriter writer, Reminder value, EncoderContext encoderContext) {
        writer.writeStartDocument();
        if (value.getId() != null) {
            writer.writeString("_id", value.getId());
        }
        writer.writeInt64("guild", value.getGuild());
        writer.writeInt64("user", value.getUser());
        writeString(writer, "reminder", value.getReminder());
        writer.writeInt64("channel", value.getChannel());
        writer.writeInt64("time", value.getTime());
        writer.writeInt64("createdAt", value.getCreatedAt());
        writer.writeEndDocument();
    }

    @Override
    public Class<Reminder> getEncoderClass() {
        return Reminder.class;
    }

    private static String readId(BsonReader reader) {
        return switch (reader.getCurrentBsonType()) {
            case STRING -> reader.readString();
            case OBJECT_ID -> reader.readObjectId().toHexString();
            case NULL -> {
                reader.readNull();
                yield null;
            }
            default -> {
                reader.skipValue();
                yield null;
            }
        };
    }

    private static String readString(BsonReader reader) {
        if (reader.getCurrentBsonType() == BsonType.NULL) {
            reader.readNull();
            return null;
        }

        return reader.readString();
    }

    private static long readLong(BsonReader reader) {
        return switch (reader.getCurrentBsonType()) {
            case INT64 -> reader.readInt64();
            case INT32 -> reader.readInt32();
            case NULL -> {
                reader.readNull();
                yield 0L;
            }
            default -> {
                reader.skipValue();
                yield 0L;
            }
        };
    }

    private static void writeString(BsonWriter writer, String name, String value) {
        if (value == null) {
            writer.writeNull(name);
        } else {
            writer.writeString(name, value);
        }
    }
}
