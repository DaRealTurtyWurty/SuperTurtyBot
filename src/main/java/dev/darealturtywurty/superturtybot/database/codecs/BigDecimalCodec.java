package dev.darealturtywurty.superturtybot.database.codecs;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.math.BigDecimal;

public class BigDecimalCodec implements Codec<BigDecimal> {
    @Override
    public void encode(BsonWriter writer, BigDecimal value, EncoderContext encoderContext) {
        writer.writeString(value.stripTrailingZeros().toPlainString());
    }

    @Override
    public BigDecimal decode(BsonReader reader, DecoderContext decoderContext) {
        return new BigDecimal(reader.readString());
    }

    @Override
    public Class<BigDecimal> getEncoderClass() {
        return BigDecimal.class;
    }
}
