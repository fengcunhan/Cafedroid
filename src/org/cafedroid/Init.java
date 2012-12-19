package org.cafedroid;
/**
 * Copyright (c) 2012-2013, fengcunhan  (fengcunhan@gmail.com).
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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cafedroid.annotation.view.ViewById;
import org.cafedroid.util.Arrays;

import android.app.Activity;
import android.content.res.Resources;

/**
 * This class is for init the view and some method
 * 
 * @author fengcunhan
 *
 */
class Init {
	private static final String TAG = "Init";

	public static void init(Activity activity) {
		Field[] fields = activity.getClass().getDeclaredFields();
		if (!Arrays.isEmpty(fields)) {
			for (Field field : fields) {
				ViewById injectView = field.getAnnotation(ViewById.class);
				if (null != injectView) {
					int id = injectView.value();
					initView(activity, field, id);
				}
			}
		}
	}

	private static void initView(Activity activity, Field field, int id) {
		try {
			if (id != 0) {
				field.setAccessible(true);
				field.set(activity, activity.findViewById(id));
			} else {
				String fieldName = field.getName();
				List<String> names = getPosiableNameOfId(fieldName);
				if (!Arrays.isEmpty(names)) {
					for (String name : names) {
						id = nameToId(name, activity);
						if (0 != id) {
							field.setAccessible(true);
							field.set(activity, activity.findViewById(id));
							break;
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static int nameToId(String name, Activity activity) {
		int id = 0;
		Resources res = activity.getResources();
		id = res.getIdentifier(name, "id", activity.getPackageName());
		return id;
	}

	/**
	 * Get all posiable name of View's id
	 * @param fieldName
	 * @return
	 */
	private static List<String> getPosiableNameOfId(String fieldName) {
		List<String> posiableNames = new ArrayList<String>();
		posiableNames.add(fieldName);
		String temp = "";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < fieldName.length(); i++) {
			char a = fieldName.charAt(i);
			temp = String.valueOf(a);
			if (temp.toUpperCase().equals(temp) && isCharacter(a)) {
				sb.append("_");
				sb.append(temp.toLowerCase());
			} else {
				sb.append(a);
			}
		}
		posiableNames.add(sb.toString());
		return posiableNames;
	}

	private static boolean isCharacter(char a) {
		Pattern pattern = Pattern.compile("[a-z]|[A-Z]");
		Matcher matcher = pattern.matcher(String.valueOf(a));
		return matcher.matches();
	}

}
