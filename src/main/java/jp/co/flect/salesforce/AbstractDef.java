package jp.co.flect.salesforce;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Set;
import java.util.Iterator;

import jp.co.flect.soap.SimpleObject;
import jp.co.flect.xmlschema.ElementDef;
import jp.co.flect.xmlschema.TypeDef;
import jp.co.flect.xmlschema.ComplexType;
import jp.co.flect.xmlschema.SimpleType;

/**
 * SObjectDef, FieldDefなどのMetadata定義体の基底クラス
 * Serialize時のサイズを減らすために小細工をしている
 */
public abstract class AbstractDef extends SimpleObject {
	
	private static final String KEY_LABEL_PLURAL   = "labelPlural";
	private static final String SHORT_LABEL_PLURAL = "lp";
	private static final String SHORT_LABEL        = "l";
	
	private static final Map<String, String> shortNameMap = new HashMap<String, String>();
	
	static {
		shortNameMap.put("byteLength", "bl");
		shortNameMap.put("childSObject", "cs");
		shortNameMap.put("digits", "d");
		shortNameMap.put("field", "f");
		shortNameMap.put("inlineHelpText", "ih");
		shortNameMap.put("keyPrefix", "kp");
		
		shortNameMap.put("label", "l");
		shortNameMap.put("labelPlural", "lp");
		
		shortNameMap.put("length", "ln");
		shortNameMap.put("name", "n");
		shortNameMap.put("precision", "p");
		shortNameMap.put("picklistValues", "pv");
		shortNameMap.put("recordTypeId", "ri");
		shortNameMap.put("referenceTo", "rt");
		shortNameMap.put("relationshipName", "r");
		shortNameMap.put("relationshipOrder", "ro");
		shortNameMap.put("scale", "s");
		shortNameMap.put("type", "t");
		shortNameMap.put("urlDetail", "ud");
		shortNameMap.put("urlEdit", "ue");
		shortNameMap.put("urlNew", "un");
	}
	
	private Metadata meta;
	private int boolSet = 0;
	
	public AbstractDef(Metadata meta) {
		this.meta = meta;
		setOptimizeLevel(OptimizeLevel.ROBUST);
	}
	
	private boolean getBool(int n) {
		int ret = boolSet >> n;
		return (ret & 0x01) == 1;
	}
	
	private void setBool(int n, boolean b) {
		if (b) {
			boolSet = boolSet | (1 << n);
		} else {
			boolSet = boolSet & ((1 << n) ^ 0xFFFF);
		}
	}
	
	@Override 
	public ComplexType getType() { return getTypeFromMetadata();}
	
	public Metadata getMetadata() { return this.meta;}
	
	protected abstract ComplexType getTypeFromMetadata();
	
	private int getBooleanIndex(String name) {
		ComplexType type = getType();
		if (type == null) {
			return -1;
		}
		int ret = 0;
		Iterator<ElementDef> it = type.modelIterator();
		while (it.hasNext()) {
			ElementDef el = it.next();
			if (el.getName().equals(name)) {
				if (el.getType().isSimpleType() && ((SimpleType)el.getType()).isBooleanType()) {
					return ret;
				}
				return -1;
			}
			if (el.getType().isSimpleType() && ((SimpleType)el.getType()).isBooleanType()) {
				ret++;
			}
		}
		return -1;
	}
	
	@Override
	public Map<String, Object> getMap() {
		Map<String, Object> map = super.getMap();
		Map<String, Object> ret = new HashMap<String, Object>(map);
		for (Map.Entry<String, String> entry : shortNameMap.entrySet()) {
			String name = entry.getKey();
			String shortName = entry.getValue();
			Object value = ret.remove(shortName);
			if (value != null) {
				ret.put(name, value);
			}
		}
		String singleLabel = (String)map.get(SHORT_LABEL);
		if (singleLabel != null && map.get(SHORT_LABEL_PLURAL) == null) {
			ret.put(KEY_LABEL_PLURAL, singleLabel);
		}
		ComplexType type = getType();
		if (type != null) {
			int idx = 0;
			Iterator<ElementDef> it = type.modelIterator();
			while (it.hasNext()) {
				ElementDef el = it.next();
				if (el.getType().isSimpleType() && ((SimpleType)el.getType()).isBooleanType()) {
					String name = el.getName();
					if (getBool(idx)) {
						ret.put(name, Boolean.TRUE);
					}
					idx++;
				}
			}
		}
		return ret;
	}
	
	@Override
	public List<String> getNameList() {
		ComplexType type = getType();
		if (type == null) {
			return super.getNameList();
		}
		TreeSet<String> set = new TreeSet<String>(super.getMap().keySet());
		for (Map.Entry<String, String> entry : shortNameMap.entrySet()) {
			String name = entry.getKey();
			String shortName = entry.getValue();
			if (set.contains(shortName)) {
				set.remove(shortName);
				set.add(name);
			}
		}
		Iterator<ElementDef> it = type.modelIterator();
		while (it.hasNext()) {
			ElementDef el = it.next();
			set.add(el.getName());
		}
		return new ArrayList(set);
	}
	
	@Override
	public boolean contains(String name) {
		return get(name) != null;
	}
	
	@Override
	public Object get(String name) {
		if (KEY_LABEL_PLURAL.equals(name)) {
			String label = (String)super.get(SHORT_LABEL_PLURAL);
			if (label != null) {
				return label;
			}
			return (String)super.get(SHORT_LABEL);
		}
		int booleanIndex = getBooleanIndex(name);
		if (booleanIndex >= 0 && booleanIndex < 32) {
			return Boolean.valueOf(getBool(booleanIndex));
		}
		String shortName = shortNameMap.get(name);
		if (shortName != null) {
			name = shortName;
		}
		return super.get(name);
	}
	
	@Override
	public void set(String name, Object value) {
		if (KEY_LABEL_PLURAL.equals(name)) {
			String label = (String)super.get(SHORT_LABEL);
			if (label != null && label.equals(value)) {
				return;
			}
			super.set(SHORT_LABEL_PLURAL, value);
			return;
		}
		int booleanIndex = getBooleanIndex(name);
		if (booleanIndex >= 0 && booleanIndex < 32) {
			boolean b = false;
			if (value instanceof Boolean) {
				b = ((Boolean)value).booleanValue();
			} else if (value != null) {
				b = Boolean.valueOf(value.toString());
			}
			setBool(booleanIndex, b);
			return;
		}
		String shortName = shortNameMap.get(name);
		if (shortName != null) {
			name = shortName;
		}
		super.set(name, value);
	}
	
	@Override
	public AbstractDef newInstance() {
		AbstractDef ret = (AbstractDef)super.newInstance();
		ret.boolSet = 0;
		return ret;
	}
}
