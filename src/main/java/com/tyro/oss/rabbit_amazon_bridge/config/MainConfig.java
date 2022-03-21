package com.tyro.oss.rabbit_amazon_bridge.config;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.fasterxml.jackson.module.kotlin.SingletonSupport;
import com.tyro.oss.rabbit_amazon_bridge.monitoring.CompositeHealthTypeAdapter;
import com.tyro.oss.rabbit_amazon_bridge.monitoring.HealthTypeAdapter;
import com.tyro.oss.rabbit_amazon_bridge.poller.SQSPollersConfigurer;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jmx.support.RegistrationPolicy;

@Configuration
@Import({TaskSchedulerConfig.class, RestConfig.class, RabbitEndPointConfigurer.class, RabbitRetryConfig.class, SQSPollersConfigurer.class})
@ComponentScan(
        value = {"com.tyro.oss.rabbit_amazon_bridge"},
        excludeFilters = {@ComponentScan.Filter({Configuration.class})}
)
@EnableMBeanExport(
        registration = RegistrationPolicy.IGNORE_EXISTING
)
@PropertySource(
        value = {"${extra.properties.file}"},
        ignoreResourceNotFound = true
)
public class MainConfig {
    @Bean
    @Primary
    @ConditionalOnProperty({"flat.healthcheck.response.format"})
    @NotNull
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new KotlinModule());
        SimpleModule module = new SimpleModule();
        module.addSerializer(Health.class, new HealthTypeAdapter());
        module.addSerializer(CompositeHealth.class, new CompositeHealthTypeAdapter());
        mapper.registerModule(module);
        return mapper;
    }

    @Bean
    @ConditionalOnMissingBean(name = {"artifactId"})
    public String artifactId(@Value("${artifact.id:undefined}") @NotNull String artifactId) {
        return artifactId;
    }

    @Bean
    @ConditionalOnMissingBean(name = {"artifactVersion"})
    public String artifactVersion(@Value("${artifact.version:undefined}") @NotNull String artifactVersion) {
        return artifactVersion;
    }
}
