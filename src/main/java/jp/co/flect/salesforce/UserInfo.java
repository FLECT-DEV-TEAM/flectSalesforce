package jp.co.flect.salesforce;

import java.util.Locale;
import java.util.TimeZone;
import java.util.Map;
import java.text.DecimalFormat;

import jp.co.flect.soap.SoapException;
import jp.co.flect.soap.SoapResponse;
import jp.co.flect.util.ExtendedMap;

public class UserInfo {
	
	public static UserInfo fromSoap(SoapResponse res) throws SoapException {
		ExtendedMap map = res.getAsMap();
		return new UserInfo((Map)map.getDeep("loginResponse.result.userInfo"), true);
	}
	
	public static UserInfo fromOAuth(ExtendedMap map) {
		return new UserInfo(map, false);
	}
	
	private static Locale getLocale(String s) {
		int idx = s.indexOf('_');
		if (idx == -1) {
			return new Locale(s);
		} else {
			String[] strs = s.split("_");
			return new Locale(strs[0], strs[1]);
		}
	}
	
	private static TimeZone getTimeZone(int offset) {
		String sign = "+";
		if (offset < 0) {
			sign = "-";
			offset = 0 - offset;
		}
		int h = offset / 1000 / 60 / 60;
		int m = offset / 1000 / 60 % 60;
		
		DecimalFormat fmt = new DecimalFormat("00");
		String id = "GMT" + sign + fmt.format(h) + fmt.format(m);
		return TimeZone.getTimeZone(id);
	}
	
	private String organizationId;
	private String userId;
	private String username;
	private String displayName;
	private String email;
	private String language;
	private Locale locale;
	private TimeZone timezone;
	
	private UserInfo(Map map, boolean soap) {
		if (soap) {
			this.organizationId = (String)map.get("organizationId");
			this.userId = (String)map.get("userId");
			this.username = (String)map.get("userName");
			this.displayName = (String)map.get("userFullName");
			this.email = (String)map.get("userEmail");
			this.language = (String)map.get("userLanguage");
			this.locale = getLocale((String)map.get("userLocale"));
			this.timezone = TimeZone.getTimeZone((String)map.get("userTimeZone"));
		} else {
			this.organizationId = (String)map.get("organization_id");
			this.userId = (String)map.get("user_id");
			this.username = (String)map.get("username");
			this.displayName = (String)map.get("display_name");
			this.email = (String)map.get("email");
			this.language = (String)map.get("language");
			this.locale = getLocale((String)map.get("locale"));
			
			int offset = ((Number)map.get("utcOffset")).intValue();
			this.timezone = getTimeZone(offset);
		}
	}
	
	public String getOrganizationId() { return this.organizationId;}
	public String getUserId() { return this.userId;}
	public String getUsername() { return this.username;}
	public String getDisplayName() { return this.displayName;}
	public String getEmail() { return this.email;}
	public String getLanguage() { return this.language;}
	public Locale getLocale() { return this.locale;}
	public TimeZone getTimeZone() { return this.timezone;}
}
