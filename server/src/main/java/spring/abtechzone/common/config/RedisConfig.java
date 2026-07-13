package spring.abtechzone.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    @Value("${REDIS_ADDRESS:redis://127.0.0.1:3308}")
    private String redisAddress;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        // kết nối tới redis server
        config.useSingleServer()
                .setAddress(redisAddress)
                .setConnectionPoolSize(10)
                .setConnectionMinimumIdleSize(5)
                .setConnectTimeout(30000);
        return Redisson.create(config);
    }
}
