package jp.co.flect.salesforce.syntax;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.text.ParseException;
import jp.co.flect.util.SimpleJson;
import jp.co.flect.salesforce.metadata.AsyncResult;
import jp.co.flect.salesforce.metadata.BaseMetadata;
import jp.co.flect.salesforce.metadata.CustomField;
import jp.co.flect.salesforce.metadata.MetadataClient;
import jp.co.flect.salesforce.metadata.MetadataType;
import jp.co.flect.soap.SoapException;
import com.google.gson.JsonSyntaxException;

public class CreateStatement extends DdlStatement {
	
	private BaseMetadata obj;
	private LinkedList<CustomField> fields;
	
	public CreateStatement(String str) throws ParseException {
		super("create");
		StringBuilder buf = new StringBuilder();
		Tokenizer t = new Tokenizer(str);
		checkToken(t, buf, getStatementName());
		MetadataType type = checkType(t, buf);
		
		try {
			SimpleJson json = SimpleJson.fromJson(str.substring(t.getIndex()));
			
			this.obj = new BaseMetadata(type);
			String objName = (String)json.get("fullName");
			
			if (type == MetadataType.CustomObject) {
				List list = (List)json.remove("fields");
				if (list != null) {
					this.fields = new LinkedList<CustomField>();
					for (Object o : list) {
						CustomField field = new CustomField();
						field.getMap().putAll((Map)o);
						field.setFullName(objName + "." + field.getFullName());
						this.fields.add(field);
					}
				}
			}
			this.obj.getMap().putAll(json);
		} catch (JsonSyntaxException e) {
			ParseException e2 = new ParseException(e.getMessage(), t.getIndex());
			e2.initCause(e);
			throw e2;
		}
	}
	
	@Override
	public List<AsyncResult> execute(MetadataClient client) throws IOException, SoapException {
		List<AsyncResult> ret = new ArrayList<AsyncResult>();
		
		AsyncResult result = client.create(this.obj);
		ret.add(result);
		if (this.fields != null) {
			List<CustomField> list = new ArrayList<CustomField>();
			while (this.fields.size() > 0) {
				while (this.fields.size() > 0 && list.size() < MetadataClient.MAX_REQUEST_COUNT) {
					list.add(this.fields.removeFirst());
				}
				try {
					ret.addAll(client.create(list));
				} catch (SoapException e) {
					AsyncResult result2 = client.checkStatus(result.getId());
					if (result2.isDone() && result2.getState() == AsyncResult.AsyncRequestState.Error) {
						ret.clear();
						result2.setRelatedMetadata(result.getRelatedMetadata());
						ret.add(result2);
					} else {
						throw e;
					}
				}
				list.clear();
			}
		}
		return ret;
	}
}
