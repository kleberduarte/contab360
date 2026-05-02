package com.contabilidade.pj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class Contab360Application {

    public static void main(String[] args) {
        SpringApplication.run(Contab360Application.class, args);
    }
}
