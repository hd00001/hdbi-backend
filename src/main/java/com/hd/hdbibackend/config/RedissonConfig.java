package com.hd.hdbibackend.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @auther hd
 * @Description
 */
@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedissonConfig {

    private Integer database;

    private String host;

    private Integer port;

    @Bean
    public RedissonClient getRedissonClient(){
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://"+host + ":" + port)
                .setDatabase(database);
        // Sync and Async API
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}
