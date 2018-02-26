package net.refractions.util;

import java.util.UUID;
import java.util.regex.Pattern;

public class UuidUtil {
	private static Pattern UUID_RE = Pattern
			.compile("\\A[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\z");
	private static Pattern UUID_RE_2 = Pattern
			.compile("\\A[0-9a-fA-F]{32}\\z");
	
	public static UUID UuidFromString(String s) {
		if(s == null || s.isEmpty()) return null;
		s = s.trim();
		if(UUID_RE.matcher(s).matches()) {
			return UUID.fromString(s);
		} else if(UUID_RE_2.matcher(s).matches()) {
			return UUID.fromString(s.substring(0,8) + "-" + s.substring(8,12) + "-" 
        			+ s.substring(12,16) + "-" + s.substring(16,20) + "-" + s.substring(20));
		}
		throw new IllegalArgumentException("Not a valid UUID string (" + s + ")");
	}
}
