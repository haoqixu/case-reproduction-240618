package com.example.demo;

import java.time.Duration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@SpringBootApplication
@RestController
public class DemoApplication {

    private JedisPool pool;

    public DemoApplication() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMinIdle(10);
        config.setMaxIdle(200);
        config.setMaxTotal(200);
        config.setMinEvictableIdleDuration(Duration.ofSeconds(2));
        config.setTimeBetweenEvictionRuns(Duration.ofSeconds(1));
        config.setTestOnBorrow(false);
        config.setTestOnReturn(false);
        config.setTestWhileIdle(false);
        config.setTestOnCreate(false);

        this.pool = new JedisPool(config, "100.100.100.100", 6379);
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @GetMapping("/hello")
    public String hello() {
        try (Jedis j = pool.getResource()) {
            j.incr("counter");
        }
        return String.format("Hello %s!", "world");
    }
}
