package com.tyro.oss.rabbit_amazon_bridge.messagetransformer;

import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;

public class DoNothingMessageTransformer implements MessageTransformer {
    @NotNull
    public String transform(@NotNull String message) {
        Intrinsics.checkNotNullParameter(message, "message");
        return message;
    }
}
