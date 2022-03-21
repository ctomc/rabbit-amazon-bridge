package com.tyro.oss.rabbit_amazon_bridge.messagetransformer;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JoltTransform;
import com.bazaarvoice.jolt.JsonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class JoltMessageTransformer implements MessageTransformer {
    final Chainr chainr;

    public JoltMessageTransformer(@NotNull List<JoltTransform> spec) {
        this.chainr = new Chainr(spec);
    }

    @Override
    public String transform(@NotNull String message) {
        Object transformedObject = this.chainr.transform(JsonUtils.jsonToObject(message));
        if (transformedObject == null) {
            transformedObject = Map.of();
        }

        return JsonUtils.toJsonString(transformedObject);
    }

    public Chainr getChainr() {
        return chainr;
    }
}
