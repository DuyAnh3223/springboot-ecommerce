package spring.abtechzone.common.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestRedisConfig {

    @Bean
    public RedissonClient redissonClient() {
        RedissonClient mockClient = mock(RedissonClient.class);
        RLock mockLock = mock(RLock.class);
        lenient().when(mockClient.getLock(anyString())).thenReturn(mockLock);
        try {
            lenient().when(mockLock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        } catch (InterruptedException e) {
            // ignore
        }
        return mockClient;
    }
}
