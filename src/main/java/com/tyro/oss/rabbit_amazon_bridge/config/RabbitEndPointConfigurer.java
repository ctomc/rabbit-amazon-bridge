package com.tyro.oss.rabbit_amazon_bridge.config;

import com.tyro.oss.rabbit_amazon_bridge.generator.Bridge;
import com.tyro.oss.rabbit_amazon_bridge.generator.BridgeGenerator;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RabbitEndPointConfigurer implements RabbitListenerConfigurer {
    private final List<Bridge> bridgesFromRabbit;
    private final BridgeGenerator bridgeGenerator;

    @Autowired
    public RabbitEndPointConfigurer(List<Bridge> bridgesFromRabbit, BridgeGenerator bridgeGenerator) {
        this.bridgesFromRabbit = bridgesFromRabbit;
        this.bridgeGenerator = bridgeGenerator;
    }

    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
        var list = bridgesFromRabbit
                .stream()
                .filter(Bridge::isForwardingMessagesEnabled)
                .toList();
        for (int i = 0; i < list.size(); i++) {
            var endpoint = bridgeGenerator.generateFromRabbit(i, list.get(i));
            registrar.registerEndpoint(endpoint);
        }
    }
}
