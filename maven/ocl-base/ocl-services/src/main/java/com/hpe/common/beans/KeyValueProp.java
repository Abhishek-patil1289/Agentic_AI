package com.hpe.common.beans;

import com.hpe.ocl.adminUI.common.BDEUtils;

public class KeyValueProp {
	
	private String key;
	private String value;
	
	public KeyValueProp() {
		super();
	}
	public KeyValueProp(String key, String value) {
		super();
		this.key = key;
		this.value = value;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key.trim();
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value.trim();
	}
}