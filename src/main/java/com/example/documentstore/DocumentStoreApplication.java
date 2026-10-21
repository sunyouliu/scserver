package com.example.documentstore;

import com.example.documentstore.config.AppProperties;
import com.example.documentstore.config.RequiredConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class DocumentStoreApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(DocumentStoreApplication.class);
        application.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event ->
                RequiredConfiguration.validate(event.getEnvironment()));
        application.run(args);
    }
}
