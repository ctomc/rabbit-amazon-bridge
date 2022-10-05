package com.tyro.oss.rabbit_amazon_bridge.monitoring;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.boot.actuate.health.CompositeHealth;

import java.io.IOException;

public class CompositeHealthTypeAdapter extends StdSerializer<CompositeHealth> {
    public CompositeHealthTypeAdapter() {
        super(CompositeHealth.class);
    }

    @Override
    public void serialize(CompositeHealth src, JsonGenerator gen, SerializerProvider ser) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("status", src.getStatus().getCode());
        var desc = src.getStatus().getDescription();
        if (desc!=null && !desc.isEmpty()){
            gen.writeStringField("description", desc);
        }

        src.getComponents().forEach((key,value) -> {
            try {
                gen.writeObjectField(key, value);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        gen.writeEndObject();
    }
}
