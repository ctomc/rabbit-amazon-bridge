package com.tyro.oss.rabbit_amazon_bridge.monitoring;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.Health;

import java.io.IOException;

public class HealthTypeAdapter extends StdSerializer<Health> {
    public HealthTypeAdapter() {
        super(Health.class);
    }

    @Override
    public void serialize(Health src, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("status", src.getStatus().getCode());
        var desc = src.getStatus().getDescription();
        if (desc!=null && !desc.isEmpty()){
            gen.writeStringField("description", desc);
        }

        src.getDetails().forEach((key,value) -> {
            try {
                gen.writeObjectField(key, value);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        gen.writeEndObject();
    }
}
