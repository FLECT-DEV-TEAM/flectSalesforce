package jp.co.flect.salesforce.bulk;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;

import jp.co.flect.log.Logger;
import jp.co.flect.log.LoggerFactory;
import jp.co.flect.net.HttpUtils;
import jp.co.flect.salesforce.Metadata;
import jp.co.flect.xml.XMLUtils;

public class BulkClient {
	
	public static final Logger log = LoggerFactory.getLogger(BulkClient.class);
	
	public static final String  API_SCHEMA = "http://www.force.com/2009/06/asyncapi/dataload";
	
	private static final String SESSION_HEADER = "X-SFDC-Session";
	
	private Metadata meta;
	private String host;
	private String apiVersion;
	private String sessionId;
	
	public BulkClient(Metadata meta, String host, String apiVersion, String sessionId) {
		this.meta = meta;
		this.host = host;
		this.apiVersion = apiVersion;
		this.sessionId = sessionId;
	}
	
	public String getHost() { return this.host;}
	
	public String getApiVersion() { return this.apiVersion;}
	public void setApiVersion(String s) { this.apiVersion = s;}
	
	public Metadata getMetadata() { return this.meta;}
	
	private String getUrl(String suffix) {
		StringBuilder buf = new StringBuilder();
		buf.append("https://")
			.append(this.host)
			.append("/services/async/")
			.append(apiVersion);
		if (suffix != null) {
			buf.append(suffix);
		}
		return buf.toString();
	}
	
	public Document getSchemaDocument() throws IOException, BulkApiException {
		HttpGet method = new HttpGet(getUrl("/AsyncApi.xsd"));
		String content = execute("getSchemaDocument", method);
		try {
			return XMLUtils.parse(new StringReader(content));
		} catch (SAXException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public JobInfo openJob(JobInfo.Operation op, String objectName) throws IOException, BulkApiException {
		JobInfo jobInfo = new JobInfo(op, objectName);
		return openJob(jobInfo);
	}
	
	public JobInfo openJob(JobInfo job) throws IOException, BulkApiException {
		HttpPost method = new HttpPost(getUrl("/job"));
		StringEntity entity = new StringEntity(job.toXML(false));
		entity.setContentType("application/xml");
		method.setEntity(entity);
		
		job.parse(execute("openJob", method));
		return job;
	}
	
	public BatchInfo addBatch(JobInfo job, File file) throws IOException, BulkApiException {
		return addBatch(job, new FileInputStream(file), file.length());
	}
	
	public BatchInfo addQuery(JobInfo job, String query) throws IOException, BulkApiException {
		String url = getUrl("/job/" + job.getId() + "/batch");
		HttpPost method = new HttpPost(url);
		method.addHeader("Content-Encoding", "gzip");
		method.addHeader("Content-Type", job.getContentType().getHeaderValue());
		method.setEntity(getGzipedEntity(query.getBytes("utf-8")));
		
		BatchInfo ret = new BatchInfo(job.getObjectName());
		ret.parse(execute("addQuery", method));
		return ret;
	}
	
	private HttpEntity getGzipedEntity(byte[] data) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		GZIPOutputStream gos = new GZIPOutputStream(bos);
		try {
			gos.write(data);
		} finally {
			gos.close();
		}
		return new ByteArrayEntity(bos.toByteArray());
	}
	
	public BatchInfo addBatch(JobInfo job, InputStream is, long length) throws IOException, BulkApiException {
		String url = getUrl("/job/" + job.getId() + "/batch");
		HttpPost method = new HttpPost(url);
		InputStreamEntity entity = new InputStreamEntity(is, length);
		if (job.getContentType() == null) {
			System.out.println("Invalid job: " + job);
		}
		entity.setContentType(job.getContentType().getHeaderValue());
		method.setEntity(entity);
		BatchInfo ret = new BatchInfo(job.getObjectName());
		ret.parse(execute("addBatch", method));
		return ret;
	}
	
	public BatchInfo getBatchStatus(BatchInfo batch) throws IOException, BulkApiException {
		String url = getUrl("/job/" + batch.getJobId() + "/batch/" + batch.getId());
		HttpGet method = new HttpGet(url);
		
		String str = execute("getBatchStatus", method);
		BatchInfo ret = new BatchInfo(batch.getObjectName());
		ret.parse(str);
		return ret;
	}
	
	public List<BatchInfo> getAllBatchStatus(JobInfo job) throws IOException, BulkApiException {
		String url = getUrl("/job/" + job.getId() + "/batch");
		HttpGet method = new HttpGet(url);
		
		String str = execute("getAllBatchStatus", method);
		List<BatchInfo> list = new ArrayList<BatchInfo>();
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory. IS_COALESCING, Boolean.TRUE);
		try {
			XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(str));
			while (reader.hasNext()) {
				int event = reader.next();
				switch (event) {
					case XMLStreamReader.START_ELEMENT:
						if (reader.getLocalName().equals("batchInfo")) {
							BatchInfo batch = new BatchInfo(job.getObjectName());
							batch.parse(reader);
							list.add(batch);
						}
						break;
				}
			}
		} catch (XMLStreamException e) {
			throw new IllegalStateException(e);
		}
		return list;
	}
	
	public List<String> getQueryResultList(BatchInfo batch) throws IOException, BulkApiException {
		String url = getUrl("/job/" + batch.getJobId() + "/batch/" + batch.getId() + "/result");
		HttpGet method = new HttpGet(url);
		
		String str = execute("getQueryResult", method);
		List<String> list = new ArrayList<String>();
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory. IS_COALESCING, Boolean.TRUE);
		try {
			XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(str));
			while (reader.hasNext()) {
				int event = reader.next();
				switch (event) {
					case XMLStreamReader.START_ELEMENT:
						if (reader.getLocalName().equals("result")) {
							list.add(reader.getElementText());
						}
						break;
				}
			}
		} catch (XMLStreamException e) {
			throw new IllegalStateException(e);
		}
		return list;
	}
	
	public void saveQueryResult(BatchInfo batch, String resultId, File file) throws IOException, BulkApiException {
		doSaveBatch("saveQueryResult", batch, file, "/result/" + resultId);
	}
	
	public void saveBatchRequest(BatchInfo batch, File file) throws IOException, BulkApiException {
		doSaveBatch("saveBatchRequest", batch, file, "/request");
	}
	
	public void saveBatchResult(BatchInfo batch, File file) throws IOException, BulkApiException {
		doSaveBatch("saveBatchResult", batch, file, "/result");
	}
	
	public SaveResultIterator batchResultIterator(BatchInfo batch) throws IOException, BulkApiException {
		String url = getUrl("/job/" + batch.getJobId() + "/batch/" + batch.getId() + "/result");
		HttpGet method = new HttpGet(url);
		method.addHeader(SESSION_HEADER, this.sessionId);
		
		long t = System.currentTimeMillis();
		HttpClient client = new DefaultHttpClient();
		try {
			HttpResponse res = client.execute(method);
			if (!HttpUtils.isResponseOk(res)) {
				error(res);
			}
			return new SaveResultIterator(res.getEntity().getContent());
		} finally {
			log.debug("{0}: {1}", "batchResultIterator", (System.currentTimeMillis() - t));
		}
	}
	
	public QueryResultIterator queryResultIterator(BatchInfo batch, String resultId) 
		throws IOException, BulkApiException 
	{
		String url = getUrl("/job/" + batch.getJobId() + "/batch/" + batch.getId() + "/result/" + resultId);
		HttpGet method = new HttpGet(url);
		method.addHeader(SESSION_HEADER, this.sessionId);
		
		long t = System.currentTimeMillis();
		HttpClient client = new DefaultHttpClient();
		try {
			HttpResponse res = client.execute(method);
			if (!HttpUtils.isResponseOk(res)) {
				error(res);
			}
			return new QueryResultIterator(this.meta, batch.getObjectName(), res.getEntity().getContent());
		} finally {
			log.debug("{0}: {1}", "queryResultIterator", (System.currentTimeMillis() - t));
		}
	}
	
	public JobInfo getJobStatus(JobInfo job) throws IOException, BulkApiException {
		String url = getUrl("/job/" + job.getId());
		HttpGet method = new HttpGet(url);
		
		JobInfo ret = new JobInfo();
		ret.parse(execute("getJobStatus", method));
		return ret;
	}
	
	public JobInfo closeJob(JobInfo job) throws IOException, BulkApiException {
		return doStateJob("closeJob", job, JobInfo.JobState.Closed);
	}
	
	public JobInfo abortJob(JobInfo job) throws IOException, BulkApiException {
		return doStateJob("abortJob", job, JobInfo.JobState.Aborted);
	}
	
	private JobInfo doStateJob(String op, JobInfo job, JobInfo.JobState state) throws IOException, BulkApiException {
		JobInfo stateJob = JobInfo.createStateJob(state);
		String url = getUrl("/job/" + job.getId());
		HttpPost method = new HttpPost(url);
		StringEntity entity = new StringEntity(stateJob.toXML(false));
		entity.setContentType("application/xml");
		method.setEntity(entity);
		
		stateJob.parse(execute(op, method));
		return stateJob;
	}
	
	private void doSaveBatch(String op, BatchInfo batch, File file, String suffix) throws IOException, BulkApiException {
		String url = getUrl("/job/" + batch.getJobId() + "/batch/" + batch.getId() + suffix);
		HttpGet method = new HttpGet(url);
		method.addHeader(SESSION_HEADER, this.sessionId);
		
		long t = System.currentTimeMillis();
		HttpClient client = new DefaultHttpClient();
		try {
			HttpResponse res = client.execute(method);
			if (!HttpUtils.isResponseOk(res)) {
				error(res);
			}
			
			FileOutputStream os = new FileOutputStream(file);
			try {
				res.getEntity().writeTo(os);
			} finally {
				os.close();
				EntityUtils.consume(res.getEntity());
			}
		} finally {
			log.debug("{0}: {1}", op, (System.currentTimeMillis() - t));
		}
	}
	
	private String execute(String op, HttpUriRequest method) throws IOException, BulkApiException {
		method.addHeader(SESSION_HEADER, this.sessionId);
		long t = System.currentTimeMillis();
		HttpClient client = new DefaultHttpClient();
		try {
			HttpResponse res = client.execute(method);
			if (!HttpUtils.isResponseOk(res)) {
				error(res);
			}
			String ret = HttpUtils.getContent(res);
			log.trace(ret);
			return ret;
		} finally {
			log.debug("{0}: {1}", op, (System.currentTimeMillis() - t));
		}
	}
	
	private void error(HttpResponse res) throws BulkApiException, IOException {
		String content = HttpUtils.getContent(res);
		try {
			Document doc = XMLUtils.parse(new StringReader(content));
			Element root = doc.getDocumentElement();
			if (root.getLocalName().equals("error")) {
				Element codeEl = XMLUtils.getElementNS(root, API_SCHEMA, "exceptionCode");
				Element msgEl = XMLUtils.getElementNS(root, API_SCHEMA, "exceptionMessage");
				throw new BulkApiException(msgEl.getTextContent(), codeEl.getTextContent());
			}
			throw new IllegalStateException(content);
		} catch (SAXException e) {
			throw new IllegalStateException(e);
		}
	}
	
}
