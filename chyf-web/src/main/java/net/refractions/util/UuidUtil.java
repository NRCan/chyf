/*
 * Copyright 2019 Government of Canada
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package net.refractions.util;

import java.util.UUID;
import java.util.regex.Pattern;

public class UuidUtil {
	private static Pattern UUID_RE = Pattern
			.compile("\\A[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\z");
	private static Pattern UUID_RE_2 = Pattern
			.compile("\\A[0-9a-fA-F]{32}\\z");
	
	public static UUID UuidFromString(String s) {
		s = s.trim();
		if(s == null || s.isEmpty()) return null;
		if(UUID_RE.matcher(s).matches()) {
			return UUID.fromString(s);
		} else if(UUID_RE_2.matcher(s).matches()) {
			return UUID.fromString(s.substring(0,8) + "-" + s.substring(8,12) + "-" 
        			+ s.substring(12,16) + "-" + s.substring(16,20) + "-" + s.substring(20));
		}
		throw new IllegalArgumentException("Not a valid UUID string (" + s + ")");
	}
}
