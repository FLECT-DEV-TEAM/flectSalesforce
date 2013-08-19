package jp.co.flect.salesforce.metadata;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.namespace.QName;

import org.apache.http.HttpResponse;
import org.xml.sax.SAXException;
import jp.co.flect.soap.SimpleObject;
import jp.co.flect.soap.SoapClient;
import jp.co.flect.soap.SoapException;
import jp.co.flect.soap.SoapResponse;
import jp.co.flect.soap.SoapTemplate;
import jp.co.flect.soap.InvalidWSDLException;
import jp.co.flect.soap.WSDL;
import jp.co.flect.template.TemplateException;
import jp.co.flect.util.ExtendedMap;
import jp.co.flect.xml.StAXConstructor;
import jp.co.flect.xml.StAXConstructException;
import jp.co.flect.xmlschema.template.DerivedHint;
import jp.co.flect.xmlschema.template.TemplateHint;
import jp.co.flect.xmlschema.template.VelocityTemplateBuilder;
import jp.co.flect.xmlschema.XMLSchemaConstants;
import jp.co.flect.xmlschema.XMLSchemaException;
import jp.co.flect.xmlschema.SimpleType;

public class MetadataClient extends SoapClient {
	
	private static final String TARGET_NAMESPACE = "http://soap.sforce.com/2006/04/metadata";
	public static final int MAX_REQUEST_COUNT = 10;
	
	private String sessionId;
	
	public MetadataClient(File wsdlFile, String sessionId, String endpoint) throws IOException, SAXException, InvalidWSDLException, XMLSchemaException {
		super(wsdlFile, new VelocityTemplateBuilder());
		init(sessionId, endpoint);
	}
	
	public MetadataClient(WSDL wsdl, String sessionId, String endpoint) {
		super(wsdl, new VelocityTemplateBuilder());
		init(sessionId, endpoint);
	}
	
	private void init(String sessionId, String endpoint) {
		setEndpoint(endpoint);
		this.sessionId = sessionId;
		
		String tns = getWSDL().getSchemaList().get(0).getTargetNamespace();
		addDefault(new QName(tns, "sharingModel"), CustomObject.SharingModel.ReadWrite.name());
		addDefault(new QName(tns, "deploymentStatus"), CustomObject.DeploymentStatus.Deployed.name());
		
		String xsd = XMLSchemaConstants.XSD_NSURI;
		addDefault(new QName(xsd, "boolean"), Boolean.FALSE);
		addDefault(new QName(xsd, "int"), 0);
		addDefault(new QName(xsd, "double"), 0.0);
	}
	
	public double getApiVersion() {
		String url = getWSDL().getEndpoint();
		int idx = url.lastIndexOf("/");
		return Double.parseDouble(url.substring(idx+1));
	}
	
	public List<FileProperties> listMetadata(MetadataType type) throws IOException, SoapException {
		return listMetadata(type, null, getApiVersion());
	}
	
	public List<FileProperties> listMetadata(MetadataType type, String folder) throws IOException, SoapException {
		return listMetadata(type, folder, getApiVersion());
	}
	
	public List<FileProperties> listMetadata(MetadataType type, String folder, double version) throws IOException, SoapException {
		ExtendedMap params = new ExtendedMap(true);
		params.putDeep("listMetadata.queries.type", type.toString());
		if (folder != null) {
			params.putDeep("listMetadata.queries.folder", folder);
		}
		params.putDeep("listMetadata.asOfVersion", Double.toString(version));
		try {
			SoapResponse res = invoke("listMetadata", null, params);
			
			StAXConstructor<FileProperties> builder = new StAXConstructor<FileProperties>(new QName(TARGET_NAMESPACE, "result"), new FileProperties());
			return builder.build(res.getAsString());
		} catch (XMLStreamException e) {
			throw new IllegalStateException(e);
		} catch (StAXConstructException e) {
			throw new IllegalStateException(e);
		} catch (TemplateException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public SoapResponse describeMetadata() throws IOException, SoapException {
		return describeMetadata(getApiVersion());
	}
	
	public SoapResponse describeMetadata(double version) throws IOException, SoapException {
		ExtendedMap params = new ExtendedMap(true);
		params.putDeep("describeMetadata.asOfVersion", Double.toString(version));
		try {
			return invoke("describeMetadata", null, params);
		} catch (TemplateException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public AsyncResult retrieve(MetadataPackage pkg) throws IOException, SoapException {
		double version = getApiVersion();
		pkg.setVersion(Double.toString(version));
		
		ExtendedMap params = new ExtendedMap(true);
		params.putDeep("retrieve.retrieveRequest.apiVersion", Double.toString(version));
		params.putDeep("retrieve.retrieveRequest.unpackaged", simpleObjectToMap(pkg));
		try {
			SoapResponse res = invoke("retrieve", null, params);
			
			StAXConstructor<AsyncResult> builder = new StAXConstructor<AsyncResult>(new QName(TARGET_NAMESPACE, "result"), new AsyncResult());
			return builder.build(res.getAsString()).get(0);
		} catch (XMLStreamException e) {
			throw new IllegalStateException(e);
		} catch (StAXConstructException e) {
			throw new IllegalStateException(e);
		} catch (TemplateException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public AsyncResult create(BaseMetadata meta) throws IOException, SoapException {
		List<AsyncResult> list = create(Arrays.asList(meta));
		return list.get(0);
	}
	
	public List<AsyncResult> create(List<? extends BaseMetadata> metas) throws IOException, SoapException {
		return doCRUD("create", metas);
	}
	
	public AsyncResult update(UpdateMetadata meta) throws IOException, SoapException {
		List<AsyncResult> list = update(Arrays.asList(meta));
		return list.get(0);
	}
	
	public List<AsyncResult> update(List<UpdateMetadata> metas) throws IOException, SoapException {
		return doCRUD("update", metas);
	}
	
	public AsyncResult delete(BaseMetadata meta) throws IOException, SoapException {
		List<AsyncResult> list = delete(Arrays.asList(meta));
		return list.get(0);
	}
	
	public List<AsyncResult> delete(List<? extends BaseMetadata> metas) throws IOException, SoapException {
		return doCRUD("delete", metas);
	}
	
	private List<AsyncResult> doCRUD(String op, List<? extends SimpleObject> metas) throws IOException, SoapException {
		boolean bUpdate = "update".equals(op);
		
		List<Map> list = new ArrayList<Map>();
		for (SimpleObject meta : metas) {
			Map<String, Object> map = simpleObjectToMap(meta);
			list.add(map);
		}
		List<TemplateHint> hintList = new ArrayList<TemplateHint>();
		hintList.add(new DerivedHint(new QName(TARGET_NAMESPACE, "Metadata"), new QName(TARGET_NAMESPACE, ((MetadataIntf)metas.get(0)).getMetadataType().toString())));
		ExtendedMap params = new ExtendedMap(true);
		params.putDeep(op + (bUpdate ? ".UpdateMetadata" : ".metadata"), list);
		try {
			SoapResponse res = invoke(op, hintList, params);
			
			StAXConstructor<AsyncResult> builder = new StAXConstructor<AsyncResult>(new QName(TARGET_NAMESPACE, "result"), new AsyncResult());
			List<AsyncResult> result = builder.build(res.getAsString());
			for (int i=0; i<Math.min(metas.size(), result.size()); i++) {
				result.get(i).setRelatedMetadata(((MetadataIntf)metas.get(i)).getMetadata());
			}
			return result;
		} catch (XMLStreamException e) {
			throw new IllegalStateException(e);
		} catch (StAXConstructException e) {
			throw new IllegalStateException(e);
		} catch (TemplateException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public List<AsyncResult> checkStatusEx(List<AsyncResult> results) throws IOException, SoapException {
		List<String> list = new ArrayList<String>(results.size());
		for (AsyncResult result : results) {
			list.add(result.getId());
		}
		return checkStatus(list);
	}
	
	public AsyncResult checkStatusEx(AsyncResult result) throws IOException, SoapException {
		return checkStatus(Arrays.asList(result.getId())).get(0);
	}
	
	public AsyncResult checkStatus(String id) throws IOException, SoapException {
		return checkStatus(Arrays.asList(id)).get(0);
	}
	
	public List<AsyncResult> checkStatus(List<String> ids) throws IOException, SoapException {
		ExtendedMap params = new ExtendedMap(true);
		params.putDeep("checkStatus.asyncProcessId", ids);
		try {
			SoapResponse res = invoke("checkStatus", null, params);
			
			StAXConstructor<AsyncResult> builder = new StAXConstructor<AsyncResult>(new QName(TARGET_NAMESPACE, "result"), new AsyncResult());
			return builder.build(res.getAsString());
		} catch (XMLStreamException e) {
			throw new IllegalStateException(e);
		} catch (StAXConstructException e) {
			throw new IllegalStateException(e);
		} catch (TemplateException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public RetrieveResult checkRetrieveStatus(String id, File outputFile) throws IOException, SoapException {
		FileOutputStream os = new FileOutputStream(outputFile);
		try {
			return checkRetrieveStatus(id, os);
		} finally {
			os.close();
		}
	}
	
	public RetrieveResult checkRetrieveStatus(String id, OutputStream os) throws IOException, SoapException {
		ExtendedMap params = new ExtendedMap(true);
		params.putDeep("checkRetrieveStatus.asyncProcessId", id);
		RetrieveStatusHandler h = new RetrieveStatusHandler(os);
		try {
			invoke("checkRetrieveStatus", null, params, h);
		} catch (TemplateException e) {
			throw new IllegalStateException(e);
		}
		return h.getResult();
	}
	
	private class RetrieveStatusHandler implements HttpResponseHandler {
		
		private RetrieveResult result;
		
		public RetrieveStatusHandler(OutputStream os) {
			this.result = new RetrieveResult(os);
		}
		
		public RetrieveResult getResult() { return this.result;}
		
		public SoapResponse handleResponse(HttpResponse res) throws IOException, SoapException {
			int code = res.getStatusLine().getStatusCode();
			if (code != 200) {
				return new SoapClient.DefaultHandler("", "checkRetrieveStatus").handleResponse(res);
			}
			StAXConstructor<RetrieveResult> builder = new StAXConstructor<RetrieveResult>(new QName(TARGET_NAMESPACE, "result"), this.result);
			try {
				this.result = builder.build(res.getEntity().getContent()).get(0);
			} catch (XMLStreamException e) {
				throw new IOException(e);
			} catch (StAXConstructException e) {
				throw new IOException(e);
			}
			return null;
		}
	}
	
	//Internal
	@Override
	public SoapResponse invoke(String op, List<TemplateHint> hints, Map params, HttpResponseHandler h) throws IOException, TemplateException, SoapException {
		params = checkParams(params);
		return super.invoke(op, hints, params, h);
	}
	
	@Override
	public SoapResponse invoke(SoapTemplate template, Map params, HttpResponseHandler h) throws IOException, TemplateException, SoapException {
		params = checkParams(params);
		return super.invoke(template, params, h);
	}
	
	private Map checkParams(Map params) {
		ExtendedMap map = null;
		if (params instanceof ExtendedMap) {
			map = (ExtendedMap)params;
		} else {
			map = new ExtendedMap(true);
			map.putAll(params);
		}
		map.putDeep("SessionHeader.sessionId", this.sessionId);
		return map;
	}
	
	private static Map<String, Object> simpleObjectToMap(SimpleObject obj) {
		Map<String, Object> map = new ExtendedMap(true);
		for (Map.Entry<String, Object> entry : obj.getMap().entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof SimpleObject) {
				value = simpleObjectToMap((SimpleObject)value);
			}
			if (value instanceof List) {
				List list = (List)value;
				if (list.size() > 0 && list.get(0) instanceof SimpleObject) {
					List<Map> newList = new ArrayList<Map>();
					for (Object child : list) {
						newList.add(simpleObjectToMap((SimpleObject)child));
					}
					value = newList;
				}
			}
			map.put(key, value);
		}
		return map;
	}
	
}
