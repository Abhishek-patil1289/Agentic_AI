package com.hp.bcs.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.hp.om.business.dropdownlist.interfaces.DropDownListService;

@Component
public class CacheConfig {

    private static final Logger LOG = LoggerFactory.getLogger(CacheConfig.class);

    private final DropDownListService dropDownListService;

    private Map<String, String> map = Collections.synchronizedMap(new HashMap<>());

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public CacheConfig(DropDownListService dropDownListService) {
        this.dropDownListService = dropDownListService;
    }

    @PostConstruct
    private void initCache() {
        try {
            rwLock.writeLock().lock();
            if (map.isEmpty()) {
                map.putAll(dropDownListService.getOmuiservicekeyValues());
                LOG.info("CacheConfig initialized with {} entries", map.size());
            }
        } catch (Exception e) {
            LOG.error("Error initializing CacheConfig", e);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Get value from cache by key.
     * If cache is empty, load from DropDownListService.
     *
     * @param key the key to lookup
     * @return the cached value or null if not found
     */
    public String getValue(String key) {
        if (key == null) {
            return null;
        }
        try {
            rwLock.readLock().lock();
            if (map.isEmpty()) {
                // Upgrade to write lock
                rwLock.readLock().unlock();
                rwLock.writeLock().lock();
                try {
                    if (map.isEmpty()) {
                        map.putAll(dropDownListService.getOmuiservicekeyValues());
                        LOG.info("CacheConfig loaded {} entries on getValue call", map.size());
                    }
                    // Downgrade to read lock
                    rwLock.readLock().lock();
                } finally {
                    rwLock.writeLock().unlock();
                }
            }
            return map.get(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Static utility method to get value from cache.
     *
     * @param key the key to lookup
     * @return the cached value or null if not found
     * @throws IllegalStateException if dropDownListService is not set
     */
    public static String getValuee(String key) {
        CacheConfig instance = getInstance();
        if (instance == null || instance.dropDownListService == null) {
            throw new IllegalStateException("CacheConfig not initialized properly with DropDownListService");
        }
        return instance.getValue(key);
    }

    /**
     * Clear and reload the cache from DropDownListService.
     */
    public void clearCache() {
        try {
            rwLock.writeLock().lock();
            map.clear();
            map.putAll(dropDownListService.getOmuiservicekeyValues());
            LOG.info("CacheConfig cache cleared and reloaded with {} entries", map.size());
        } catch (Exception e) {
            LOG.error("Error clearing CacheConfig cache", e);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    // Singleton instance for static access
    private static volatile CacheConfig cacheConfigInstance;

    /**
     * Get singleton instance of CacheConfig.
     * This method assumes Spring context has initialized CacheConfig bean.
     *
     * @return CacheConfig instance
     */
    public static CacheConfig getInstance() {
        if (cacheConfigInstance == null) {
            synchronized (CacheConfig.class) {
                if (cacheConfigInstance == null) {
                    // In real scenarios, this should be obtained from Spring context
                    cacheConfigInstance = new CacheConfig(null);
                }
            }
        }
        return cacheConfigInstance;
    }

    /**
     * Set the singleton instance.
     * Used by Spring to inject the created bean.
     *
     * @param instance the CacheConfig instance
     */
    public static void setInstance(CacheConfig instance) {
        cacheConfigInstance = instance;
    }

}