/**
 * Copyright (c) 2012-2013, Michael Yang 杨福海 (www.yangfuhai.com).
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

import java.io.File;

import org.cafedroid.util.BitmapCommonUtils;
import org.cafedroid.util.FileNameGenerator;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;

public class BitmapCache {
	private static final String TAG = "ImageCache";

	// 默认的内存缓存大小
	private static final int DEFAULT_MEM_CACHE_SIZE = 1024 * 1024 * 8; // 8MB

	// 默认的磁盘缓存大小
	private static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 20; // 20MB

	// Compression settings when writing images to disk cache
	private static final CompressFormat DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG;
	private static final int DEFAULT_COMPRESS_QUALITY = 70;
	// Constants to easily toggle various caches
	private static final boolean DEFAULT_MEM_CACHE_ENABLED = true;
	private static final boolean DEFAULT_DISK_CACHE_ENABLED = true;
	private static final boolean DEFAULT_CLEAR_DISK_CACHE_ON_START = false;
	private static final boolean DEFAULT_INIT_DISK_CACHE_ON_CREATE = true;

	private LruDiskCache mDiskLruCache;
	private LruMemoryCache<String, Bitmap> mMemoryCache;
	private ImageCacheParams mCacheParams;
	private final Object mDiskCacheLock = new Object();
	private boolean mDiskCacheStarting = true;

	private Context mContext;

	/**
	 * Creating a new ImageCache object using the specified parameters.
	 * 
	 * @param cacheParams
	 *            The cache parameters to use to initialize the cache
	 */
	public BitmapCache(ImageCacheParams cacheParams, Context context) {
		init(cacheParams);
		this.mContext = context;
	}

	/**
	 * Initialize the cache, providing all parameters.
	 * 
	 * @param cacheParams
	 *            The cache parameters to initialize the cache
	 */
	private void init(ImageCacheParams cacheParams) {
		// By default the disk cache is not initialized here as it should be
		// initialized
		// on a separate thread due to disk access.
		
		mCacheParams = cacheParams;
		
		if (cacheParams.initDiskCacheOnCreate) {
			// Set up disk cache
			initDiskCache();
		}
		// Set up memory cache
		if (mCacheParams.memoryCacheEnabled) {
			mMemoryCache = new LruMemoryCache<String, Bitmap>(
					mCacheParams.memCacheSize, mDiskLruCache) {
				/**
				 * Measure item size in bytes rather than units which is more
				 * practical for a bitmap cache
				 */
				@Override
				protected int sizeOf(String key, Bitmap bitmap) {
					return BitmapCommonUtils.getBitmapSize(bitmap);
				}
			};
		}

	}

	/**
	 * Initializes the disk cache. Note that this includes disk access so this
	 * should not be executed on the main/UI thread. By default an ImageCache
	 * does not initialize the disk cache when it is created, instead you should
	 * call initDiskCache() to initialize it on a background thread.
	 */
	public void initDiskCache() {
		// Set up disk cache
		synchronized (mDiskCacheLock) {
			if (mDiskLruCache == null) {
				File diskCacheDir = mCacheParams.diskCacheDir;
				if (mCacheParams.diskCacheEnabled && diskCacheDir != null) {
					if (!diskCacheDir.exists()) {
						diskCacheDir.mkdirs();
					}
					if (BitmapCommonUtils.getUsableSpace(diskCacheDir) > mCacheParams.diskCacheSize) {

						mDiskLruCache = LruDiskCache.openCache(mCacheParams);

					}
				}
			}
			mDiskCacheStarting = false;
			mDiskCacheLock.notifyAll();
		}
	}

	/**
	 * Adds a bitmap to both memory and disk cache.
	 * 
	 * @param data
	 *            Unique identifier for the bitmap to store
	 * @param bitmap
	 *            The bitmap to store
	 */
	public void put(String data, Bitmap bitmap) {
		if (data == null || bitmap == null) {
			return;
		}
		String key=FileNameGenerator.generator(data);
		// Add to memory cache
		if (mMemoryCache != null && mMemoryCache.get(key) == null) {
			mMemoryCache.put(key, bitmap);
		}
	}

	/**
	 * Get from memory cache.
	 * 
	 * @param data
	 *            Unique identifier for which item to get
	 * @return The bitmap if found in cache, null otherwise
	 */
	public Bitmap get(String data) {
		String key=FileNameGenerator.generator(data);
		Bitmap memBitmap = null;
		if (mMemoryCache != null) {
			memBitmap = mMemoryCache.get(key);
			if (memBitmap != null) {
				return memBitmap;
			} else {
				memBitmap = mDiskLruCache.get(key);
			}
		}
		return memBitmap;
	}

	/**
	 * Clears both the memory and disk cache associated with this ImageCache
	 * object. Note that this includes disk access so this should not be
	 * executed on the main/UI thread.
	 */
	public void clearCache() {
		if (mMemoryCache != null) {
			mMemoryCache.clean();
		}

		synchronized (mDiskCacheLock) {
			mDiskCacheStarting = true;
			if (mDiskLruCache != null) {
				mDiskLruCache.clean();
			}
		}
	}

	/**
	 * A holder class that contains cache parameters.
	 */
	public static class ImageCacheParams {
		public int memCacheSize = DEFAULT_MEM_CACHE_SIZE;
		public int diskCacheSize = DEFAULT_DISK_CACHE_SIZE;
		public File diskCacheDir;
		public CompressFormat compressFormat = DEFAULT_COMPRESS_FORMAT;
		public int compressQuality = DEFAULT_COMPRESS_QUALITY;
		public boolean memoryCacheEnabled = DEFAULT_MEM_CACHE_ENABLED;
		public boolean diskCacheEnabled = DEFAULT_DISK_CACHE_ENABLED;
		public boolean clearDiskCacheOnStart = DEFAULT_CLEAR_DISK_CACHE_ON_START;
		public boolean initDiskCacheOnCreate = DEFAULT_INIT_DISK_CACHE_ON_CREATE;

		public ImageCacheParams(File diskCacheDir) {
			this.diskCacheDir = diskCacheDir;
		}

		public ImageCacheParams(String diskCacheDir) {
			this.diskCacheDir = new File(diskCacheDir);
		}
		
		/**
		 * 设置缓存大小
		 * 
		 * @param context
		 * @param percent
		 *            百分比，值的范围是在 0.05 到 0.8之间
		 */
		public void setMemCacheSizePercent(Context context, float percent) {
			if (percent < 0.05f || percent > 0.8f) {
				throw new IllegalArgumentException(
						"setMemCacheSizePercent - percent must be "
								+ "between 0.05 and 0.8 (inclusive)");
			}
			memCacheSize = Math.round(percent * getMemoryClass(context) * 1024
					* 1024);
		}

		public void setMemCacheSize(int memCacheSize) {
			this.memCacheSize = memCacheSize;
		}

		public void setDiskCacheSize(int diskCacheSize) {
			this.diskCacheSize = diskCacheSize;
		}

		private static int getMemoryClass(Context context) {
			return ((ActivityManager) context
					.getSystemService(Context.ACTIVITY_SERVICE))
					.getMemoryClass();
		}
	}

}
