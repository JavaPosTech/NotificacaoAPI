package br.com.fiap.notificacaoapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableRetry
@EnableScheduling
@SpringBootApplication
public class NotificacaoAPIApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificacaoAPIApplication.class, args);
    }

}
