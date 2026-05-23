package com.yupi.yuaicodemother.config;

import cn.hutool.core.util.StrUtil;
import com.yupi.yuaicodemother.ai.memory.ResilientChatMemoryStore;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedisChatMemoryStoreConfig {

    private String host;

    private int port;

    private String password;

    private long ttl;

    @Bean
    public RedisChatMemoryStore redisChatMemoryStore() {
        RedisChatMemoryStore.Builder builder = RedisChatMemoryStore.builder()
                .host(host)
                .port(port)
                .password(password)
                .ttl(ttl);
        if (StrUtil.isNotBlank(password)) {
            builder.user("default");
        }
        return builder.build();
    }

    @Bean
    public ChatMemoryStore chatMemoryStore(RedisChatMemoryStore redisChatMemoryStore) {
        return new ResilientChatMemoryStore(redisChatMemoryStore);
    }
}
