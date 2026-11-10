package com.comidas.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Habilita el sistema de caché de Spring.
 * Por defecto usa ConcurrentMapCacheManager (en memoria).
 * En producción se puede reemplazar por Redis u otro proveedor.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
