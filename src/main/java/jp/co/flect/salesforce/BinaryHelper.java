package jp.co.flect.salesforce;

public class BinaryHelper {
	
	private SObjectDef obj;
	private FieldDef binaryField;
	
	public BinaryHelper(SObjectDef obj) {
		this(obj, null);
	}
	
	public BinaryHelper(SObjectDef obj, String fieldName) {
		this.obj = obj;
		if (fieldName == null) {
			for (FieldDef fd : obj.getFieldList()) {
				if (fd.getSoapType().isBinaryType()) {
					fieldName = fd.getName();
					break;
				}
			}
		} 
		if (fieldName != null) {
			this.binaryField = obj.getField(fieldName);
		}
	}
	
	public SObjectDef getObjectDef() { return this.obj;}
	public FieldDef getBinaryField() { return this.binaryField;}
	
	public boolean hasBinaryField() { return this.binaryField != null;}
	
	private boolean isBody() {
		return "Body".equals(getBinaryFieldName());
	}
	
	private boolean isContentData() {
		return "ContentData".equals(getBinaryFieldName());
	}
	
	private String getFromArray(String[] array) {
		for (String s : array) {
			if (obj.getField(s) != null) {
				return s;
			}
		}
		return null;
	}
	
	public String getBinaryFieldName() {
		return this.binaryField != null ? this.binaryField.getName() : null;
	}
	
	public String getLengthFieldName() {
		String ret = isBody() ? "BodyLength" : isContentData() ? "ContentSize" : null;
		if (ret != null) {
			return this.obj.getField(ret) != null ? ret : null;
		}
		return getFromArray(new String[] {
			"BodyLength",
			"ContentSize"
		});
	}
	
	public String getContentTypeFieldName() {
		String ret = "ContentType";
		return this.obj.getField(ret) != null ? ret : null;
	}
	
	public String getFilenameFieldName() {
		String ret = isBody() ? "Name" : isContentData() ? "ContentFilename" : null;
		if (ret != null) {
			return this.obj.getField(ret) != null ? ret : null;
		}
		return getFromArray(new String[] {
			"Filename",
			"ContentFilename",
			"Name"
		});
	}
	
	public boolean isRequired() {
		if (this.binaryField == null) {
			return false;
		}
		if ("QuoteDocument".equals(this.obj.getName())) {
			return true;
		}
		return this.binaryField.isRequired();
	}
	
}
