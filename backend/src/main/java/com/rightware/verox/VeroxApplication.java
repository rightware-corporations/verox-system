package com.rightware.verox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class VeroxApplication {

    public static void main(String[] args) {
        SpringApplication.run(VeroxApplication.class, args);
    }
}
