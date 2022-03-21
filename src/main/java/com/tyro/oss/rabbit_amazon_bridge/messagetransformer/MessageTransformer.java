package com.tyro.oss.rabbit_amazon_bridge.messagetransformer;

import org.jetbrains.annotations.NotNull;

public interface MessageTransformer {
    public String transform(@NotNull String message);
}
