package com.hp.bcs.utils;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.hp.om.business.dropdownlist.interfaces.DropDownListService;

public class CacheConfig {

	private static Map<String, String> map = new HashMap<>();

	private static CacheConfig cacheConfig;

	private static DropDownListService dropDownListService;

	private CacheConfig() {

	}

	public static CacheConfig getInstance() {

		if (cacheConfig == null) {
			cacheConfig = new CacheConfig();
		}
		return cacheConfig;
	}

	@Autowired
	public CacheConfig(DropDownListService dropDownListService) {
		CacheConfig.dropDownListService = dropDownListService;
	}

	public String getValue(String key) {
		if (map.isEmpty()) {
			map = dropDownListService.getOmuiservicekeyValues();
		}
		return map.get(key);
	}

	public static String getValuee(String key) {
		if (map.isEmpty()) {
			map = dropDownListService.getOmuiservicekeyValues();
		}
		return map.get(key);
	}

	public void clearCache() {

		if (!map.isEmpty()) {
			map.clear();
			map.putAll(dropDownListService.getOmuiservicekeyValues());
		}
	}
private static Map<String, String> map = new HashMap<>();

    private static CacheConfig cacheConfig;

    private static DropDownListService dropDownListService;

    private CacheConfig() {

    }

    public static CacheConfig getInstance() {

        if (cacheConfig == null) {
            cacheConfig = new CacheConfig();
        }
        return cacheConfig;
    }

    @Autowired
    public CacheConfig(DropDownListService dropDownListService) {
        CacheConfig.dropDownListService = dropDownListService;
    }

    public String getValue(String key) {
        if (map.isEmpty()) {
            map = dropDownListService.getOmuiservicekeyValues();
        }
        return map.get(key);
    }

    public static String getValuee(String key) {
        if (map.isEmpty()) {
            map = dropDownListService.getOmuiservicekeyValues();
        }
        return map.get(key);
    }

    public void clearCache() {

        if (!map.isEmpty()) {
            map.clear();
            map.putAll(dropDownListService.getOmuiservicekeyValues());
        }
    }
}
