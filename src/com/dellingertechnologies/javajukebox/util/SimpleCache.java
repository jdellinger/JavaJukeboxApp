package com.dellingertechnologies.javajukebox.util;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SimpleCache<T> {

	private long defaultExpires;
	private final Map<String, SoftReference<T>> objects;
	private final Map<String, Long> expires;
	
	public SimpleCache(){
		this(60);
	}
	public SimpleCache(long defaultExpires){
		this.defaultExpires = defaultExpires;
		objects = Collections.synchronizedMap(new HashMap<String, SoftReference<T>>());
		expires = Collections.synchronizedMap(new HashMap<String, Long>());
	}
	
	public void put(String key, T value){
		put(key, value, defaultExpires);
	}
	
	public void put(String key, T value, long expires){
		this.objects.put(key, new SoftReference<T>(value));
		this.expires.put(key, System.currentTimeMillis() + 	expires*1000);
	}

	public T get(String key){
		Long expireTime = expires.get(key);
		if(expireTime == null) return null;
		if(System.currentTimeMillis() > expireTime){
			remove(key);
			return null;
		}
		return objects.get(key).get();
	}
	
	private void remove(String key) {
		objects.remove(key);
		expires.remove(key);
	}
}
