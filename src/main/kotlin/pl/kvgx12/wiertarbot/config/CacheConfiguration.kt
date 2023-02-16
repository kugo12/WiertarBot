package pl.kvgx12.wiertarbot.config

import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CacheConfiguration {
    @Bean
    fun cacheManager() = CaffeineCacheManager("permissions")
}