package com.tyro.oss.rabbit_amazon_bridge.generator;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.exception.SpecException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Component
public class BridgeConfigFileParser {
    private Logger LOG = LoggerFactory.getLogger(BridgeConfigFileParser.class);
    private ObjectMapper objectMapper;
    private List<Resource> bridgeConfigResources;

    @Autowired
    public BridgeConfigFileParser(ObjectMapper objectMapper,
                                  List<Resource> bridgeConfigResources) {
        this.objectMapper = objectMapper;
        this.bridgeConfigResources = bridgeConfigResources;
    }

    public List<Bridge> parse() {
        var bridges = bridgeConfigResources
                .stream()
                .map(resource -> {
                    try {
                        return (List<Bridge>) objectMapper.readValue(resource.getInputStream(), objectMapper.getTypeFactory()
                                                                                                            .constructCollectionType(List.class, Bridge.class));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .flatMap(Collection::stream)
                .toList();


        check(!bridges.isEmpty(), "Bridge config should be defined");
        /*check(bridges.all { it.to != null }) { "'To' definition is required" }
        check(bridges.all { it.from != null }) { "A 'from' definition is required" }*/

        Bridge.fromRabbit(bridges)
              .forEach(bridge -> {
                  check(hasAValidJoltSpecIfPresent(bridge), "Invalid transformationSpec");
                  if (bridge.to() == null ){
                      throw new IllegalStateException("To definition needs to be set");
                  }
                  if (bridge.to().sqs() == null && bridge.to().sns() == null){
                      throw new IllegalStateException("An SNS or SQS definition is required if messages are coming from rabbit");
                  }
              });


        Bridge.fromSqs(bridges)
              .forEach(bridge -> {
                  check(!(bridge.to().sqs() != null || bridge.to().sns() != null), "Forwarding SQS to SQS/SNS is not supported");
                  check(bridge.to().rabbit() != null, "An rabbit definition is required for messages coming from SQS");
              });


        bridges.forEach(bridge -> check(!(bridge.to().sqs() != null && bridge.to().sns() != null), "We do not currently support fanout to multiple AWS destinations in one bridge"));


        return bridges;
    }

    private void check(boolean value, String message) {
        if (!value) {
            throw new IllegalStateException(message);
        }
    }

    private boolean hasAValidJoltSpecIfPresent(Bridge it) {
        if (it.transformationSpecs() == null || it.transformationSpecs().isEmpty() ) {
            return true;
        }
        try {
            Chainr.fromSpec(it.transformationSpecs());
            return true;
        } catch (SpecException e) {
            LOG.error("The provided jolt spec is invalid", e);
            return false;
        }

    }
}
