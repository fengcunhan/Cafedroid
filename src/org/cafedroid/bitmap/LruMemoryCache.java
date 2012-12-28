/**
 * Copyright (c) 2012-2013, fengcunhan(fengcunhan@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cafedroid.bitmap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.graphics.Bitmap;
import android.os.Handler;

public class LruMemoryCache<K, V> implements Cache<K, V> {
	private final LinkedHashMap<K, V> map;

	/** Size of this cache in units. Not necessarily the number of elements. */
	private int size;
	private int maxSize;
	private LruDiskCache<String, Bitmap> mDiskLruCache;
	
	private Handler mHandler = new Handler();

	private final Object mMenoryCacheLock = new Object();

	private Pattern pattern;

	/**
	 * @param maxSize
	 *            for caches that do not override {@link #sizeOf}, this is the
	 *            maximum number of entries in the cache. For all other caches,
	 *            this is the maximum sum of the sizes of the entries in this
	 *            cache.
	 */
	public LruMemoryCache(int maxSize,
			LruDiskCache<String, Bitmap> mDiskLruCache) {
		if (maxSize <= 0) {
			throw new IllegalArgumentException("maxSize <= 0");
		}
		this.maxSize = maxSize;
		this.map = new LinkedHashMap<K, V>(0, 0.75f, true);
		this.mDiskLruCache = mDiskLruCache;
		pattern = Pattern.compile("[0-9]+");
	}

	/**
	 * Returns the size of the entry for {@code key} and {@code value} in
	 * user-defined units. The default implementation returns 1 so that size is
	 * the number of entries and max size is the maximum number of entries.
	 * <p>
	 * An entry's size must not change while it is in the cache.
	 */
	protected int sizeOf(K key, V value) {
		return 1;
	}

	@Override
	public boolean exists(K key) {
		return map.containsKey(key);
	}

	@Override
	public void put(final K key, V value) {
		if (key == null || value == null) {
			throw new NullPointerException("put method key or value is null");
		}
		synchronized (mMenoryCacheLock) {
			map.put(key, value);
			size += safeSizeOf(key, value);
			trimToSize();
		}
	}

	@Override
	public void remove(final K key) {
		if (key == null) {
			throw new NullPointerException("remove method key value is null");
		}
		synchronized (mMenoryCacheLock) {
			V value = map.get(key);
			size -= safeSizeOf(key, value);
			if (value instanceof Bitmap && key instanceof String) {
				final Bitmap bitmap = (Bitmap) value;
				Matcher matcher = pattern.matcher((String) key);
				if (!matcher.matches()) {
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							if (!mDiskLruCache.exists((String) key)) {
								mDiskLruCache.put((String) key, bitmap);
							}
						}
					});
				}
			}
			map.remove(key);
		}
	}

	@Override
	public V get(K key) {
		if (key == null) {
			throw new NullPointerException("get method key value is null");
		}
		synchronized (mMenoryCacheLock) {
			V value = map.get(key);
			if (null == value) {
				boolean isExists=mDiskLruCache.exists((String) key);
				if(isExists){
					value = (V) mDiskLruCache.get((String) key);
					put(key, value);//put the value in to memory cache
				}
			}
			return value;
		}
	}

	@Override
	public void update(K key, V value) {
		if (key == null || value == null) {
			throw new NullPointerException("put method key or value is null");
		}
		synchronized (mMenoryCacheLock) {
			// Remove the old one
			V oldValue = map.get(key);
			size -= safeSizeOf(key, oldValue);
			map.remove(key);
			if (oldValue instanceof Bitmap) {
				Bitmap map = (Bitmap) oldValue;
				if (!map.isRecycled()) {
					map.recycle();
					map = null;
				}
			}
			put(key, value);
			size += safeSizeOf(key, value);
			trimToSize();
		}

	}

	@Override
	public void clean() {
		if (null != map) {
			synchronized (mMenoryCacheLock) {
				while (map.size() > 0) {
					final K entry = map.keySet().iterator().next();
					V value = map.get(entry);
					if (value instanceof Bitmap && entry instanceof String) {
						final Bitmap bitmap = (Bitmap) value;
						mHandler.post(new Runnable() {
							@Override
							public void run() {
								mDiskLruCache.put((String) entry, bitmap);
								if (!bitmap.isRecycled()) {
									bitmap.recycle();
								}
							}
						});
					}
				}
			}
		}
	}

	/**
	 * @param maxSize
	 *            the maximum size of the cache before returning. May be -1 to
	 *            evict even 0-sized elements.
	 */
	private void trimToSize() {
		while (true) {
			final K key;
			V value;
			synchronized (mMenoryCacheLock) {
				if (size < 0 || (map.isEmpty() && size != 0)) {
					throw new IllegalStateException(getClass().getName()
							+ ".sizeOf() is reporting inconsistent results!");
				}
				if (size <= maxSize || map.isEmpty()) {
					break;
				}
				Map.Entry<K, V> toEvict = map.entrySet().iterator().next();
				key = toEvict.getKey();
				value = toEvict.getValue();
				map.remove(key);
				size -= safeSizeOf(key, value);
				if (value instanceof Bitmap) {
					final Bitmap bitmap = (Bitmap) value;
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							mDiskLruCache.put((String) key, bitmap);
							if (!bitmap.isRecycled()) {
								bitmap.recycle();
							}
						}
					});
				}
			}
		}
	}

	private int safeSizeOf(K key, V value) {
		int result = sizeOf(key, value);
		if (result < 0) {
			throw new IllegalStateException("Negative size: " + key + "="
					+ value);
		}
		return result;
	}

	@Override
	public void delete(K key) {

	}

}
