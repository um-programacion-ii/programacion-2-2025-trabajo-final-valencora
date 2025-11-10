package com.um.eventosbackend;

import com.um.eventosbackend.config.AsyncSyncConfiguration;
import com.um.eventosbackend.config.EmbeddedElasticsearch;
import com.um.eventosbackend.config.EmbeddedKafka;
import com.um.eventosbackend.config.EmbeddedSQL;
import com.um.eventosbackend.config.JacksonConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = { BackendApp.class, JacksonConfiguration.class, AsyncSyncConfiguration.class })
@EmbeddedElasticsearch
@EmbeddedSQL
@EmbeddedKafka
public @interface IntegrationTest {
}
