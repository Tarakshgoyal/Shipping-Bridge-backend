package com.example.demo.config;

import com.example.demo.service.CacheNames;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

	@Bean
	public CacheManager cacheManager() {
		return new ConcurrentMapCacheManager(
				CacheNames.SHIPPING_RATES,
				CacheNames.ORDER_DETAILS,
				CacheNames.ORDER_RECORDS,
				CacheNames.TRACKING);
	}
}
