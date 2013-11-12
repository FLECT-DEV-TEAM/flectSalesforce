package jp.co.flect.salesforce.fixtures;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import org.yaml.snakeyaml.Yaml;
import jp.co.flect.xmlschema.SimpleType;

public class FixtureLoader {
	
	public List<Fixture> load(File f) throws IOException {
		List<Fixture> list = new ArrayList<Fixture>();
		InputStream input = new FileInputStream(f);
		try {
			Yaml yaml = new Yaml();
			Map map = (Map)yaml.load(input);
			
			for (Object o : map.entrySet()) {
				Map.Entry entry = (Map.Entry)o;
				String name = (String)entry.getKey();
				Map values = (Map)entry.getValue();
				list.add(parse(name, values));
			}
		} finally {
			input.close();
		}
		return list;
	}
	
	private Fixture parse(String name, Map map) throws IOException {
		String objectName = (String)map.remove("$object");
		if (objectName == null) {
			throw new IOException("$object is not specified");
		}
		String key = (String)map.remove("$key");
		if (key == null) {
			throw new IOException("$key is not specified");
		}
		String[] keys = key.split("=");
		if (keys.length != 2) {
			throw new IOException("Invalid $key value: " + key);
		}
		Fixture fx = new Fixture(name, objectName, keys[0], keys[1]);
		for (Object o : map.entrySet()) {
			Map.Entry entry = (Map.Entry)o;
			String fkey = entry.getKey().toString();
			Object value = entry.getValue();
			if (fkey.startsWith("$")) {
				if (value == null) {
					continue;
				}
				if ("$delete".equals(fkey)) {
					fx.setCanDelete(Boolean.valueOf(value.toString()));
				} else if ("$desc".equals(fkey) || "$description".equals(fkey)) {
					fx.setDescription(value.toString());
				} else {
					fx.addProperty(fkey.substring(1), value);
				}
			} else {
				if (value != null) {
					if (value instanceof Date) {
						Date d = (Date)value;
						value = hasTime(d) ? SimpleType.DATETIME.format(d) : SimpleType.DATE.format(d);
					} else {
						value = value.toString();
					}
				}
				fx.addFieldValue(fkey, (String)value);
			}
		}
		return fx;
	}
	
	private static boolean hasTime(Date d) {
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.setTime(d);
		return cal.get(Calendar.HOUR_OF_DAY) != 0 ||
		       cal.get(Calendar.MINUTE) != 0 ||
		       cal.get(Calendar.SECOND) != 0 ||
		       cal.get(Calendar.MILLISECOND) != 0;
	}
}
