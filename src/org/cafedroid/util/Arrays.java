package org.cafedroid.util;
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
import java.util.List;

public class Arrays {
	public static boolean isEmpty(Object[] objects){
		boolean isEmpty=false;
		if(null==objects){
			isEmpty=true;
		}else{
			if(objects.length==0){
				isEmpty=true;
			}
		}
		return isEmpty;
	}
	
	public static boolean isEmpty(List objects){
		boolean isEmpty=false;
		if(null==objects){
			isEmpty=true;
		}else{
			if(objects.size()==0){
				isEmpty=true;
			}
		}
		return isEmpty;
	}
}
