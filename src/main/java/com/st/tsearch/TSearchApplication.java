package com.st.tsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(TSearchApplication.class, args);
    }

}
