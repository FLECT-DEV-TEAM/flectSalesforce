package jp.co.flect.salesforce.query;

import java.util.ArrayList;
import java.util.List;
import javax.swing.event.EventListenerList;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;

import jp.co.flect.salesforce.Metadata;
import jp.co.flect.salesforce.SObject;
import jp.co.flect.salesforce.event.SObjectListener;
import jp.co.flect.salesforce.event.SObjectEvent;
import jp.co.flect.util.StringUtils;
import static jp.co.flect.util.StringUtils.checkNull;
import jp.co.flect.xml.StAXConstruct;
import jp.co.flect.xml.StAXConstructException;

public class QueryResult<T extends SObject> implements StAXConstruct<QueryResult<T>>, Cloneable {
	
	private Metadata meta;
	private T baseObject;
	
	private boolean done;
	private String queryLocator;
	private List<T> records = new ArrayList<T>();
	private int allSize;
	private BuildOption option = null;
	private EventListenerList listeners = null;
	private QueryFilter filter = null;
	
	public QueryResult(Metadata meta, T obj) {
		this.meta = meta;
		this.baseObject = obj;
	}
	
	private void addBaseObject() {
		this.records.add(this.baseObject);
	}
	
	public boolean isDone() { return this.done;}
	public String getQueryLocator() { return this.queryLocator;}
	
	public List<T> getRecords() { return this.records;}
	public T getRecord(int idx) { return this.records.get(idx);}
	
	public int getCurrentSize() { return this.records.size();}
	public int getAllSize() { return this.allSize;}
	
	public QueryFilter getFilter() { return this.filter;}
	public void setFilter(QueryFilter filter) { this.filter = filter;}
	
	public int getSkipRecords() { return this.option == null ? 0 : this.option.skipRecords;}
	public void setSkipRecords(int n) {
		if (this.option == null) {
			this.option = new BuildOption();
		}
		this.option.skipRecords = n;
	}
	
	public void addMoreResult(QueryResult<T> result) {
		this.done = result.done;
		this.queryLocator = result.queryLocator;
		this.records.addAll(result.records);
		this.allSize = result.allSize;
	}
	
	public int getMaxRecords() { return this.option == null ? 0 : this.option.maxRecords;}
	public void setMaxRecords(int n) {
		if (this.option == null) {
			this.option = new BuildOption();
		}
		this.option.maxRecords = n;
	}
	
	private boolean matchBuildOption() {
		if (option == null) {
			return true;
		}
		if (option.skipCnt < option.skipRecords) {
			option.skipCnt++;
			return false;
		}
		if (option.maxRecords > 0 && this.records.size() == option.maxRecords) {
			return false;
		}
		return true;
	}
	
	public void build(XMLStreamReader reader) throws XMLStreamException, StAXConstructException {
		build(reader, 1);
	}
	
	private void build(XMLStreamReader reader, int depth) throws XMLStreamException, StAXConstructException {
		String targetNamespace = this.meta.getMessageURI();
		
		boolean finished = false;
		while (!finished && reader.hasNext()) {
			int event = reader.next();
			switch (event) {
				case XMLStreamReader.START_ELEMENT:
				{
					String nsuri = reader.getNamespaceURI();
					String name = reader.getLocalName();
					if (targetNamespace.equals(nsuri)) {
						if ("records".equals(name)) {
							if (matchBuildOption()) {
								T obj = (T)this.baseObject.newInstance();
								obj.build(reader);
								if (filter == null || filter.filter(obj)) {
									fireEvent(obj);
									this.records.add(obj);
								}
							} else {
								skipElement(reader);
							}
						} else if ("done".equals(name)) {
							this.done = Boolean.valueOf(reader.getElementText()).booleanValue();
						} else if ("queryLocator".equals(name)) {
							String value = reader.getElementText();
							if (value != null && value.length() > 0) {
								this.queryLocator = value;
							}
						} else if ("size".equals(name)) {
							this.allSize = Integer.parseInt(reader.getElementText());
						} else {
							depth++;
						}
					} else {
						depth++;
					}
					break;
				}
				case XMLStreamReader.END_ELEMENT:
				{
					depth--;
					if (depth == 0) {
						finished = true;
					}
					break;
				}
			}
		}
	}
	
	private void skipElement(XMLStreamReader reader) throws XMLStreamException {
		int depth = 0;
		while (true) {
			int n = reader.next();
			switch (n) {
				case XMLStreamReader.START_ELEMENT:
					depth++;
					break;
				case XMLStreamReader.END_ELEMENT:
					if (depth == 0) {
						return;
					} else {
						depth--;
					}
					break;
			}
		}
	}
	
	public QueryResult<T> newInstance() {
		try {
			QueryResult<T> ret = (QueryResult<T>)super.clone();
			ret.records = new ArrayList<T>();
			if (this.option != null) {
				ret.setSkipRecords(this.getSkipRecords());
				ret.setMaxRecords(this.getMaxRecords());
			}
			return ret;
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buildString(buf, 0);
		return buf.toString();
	}
	
	public void buildString(StringBuilder buf, int indent) {
		String strIndent = StringUtils.getSpace(indent);
		buf.append(strIndent).append("done: ").append(this.done)
			.append("\n").append(strIndent).append("queryLocator: ").append(this.queryLocator)
			.append("\n").append(strIndent).append("AllSize: ").append(this.allSize)
			.append("\n").append(strIndent).append("RecordSize: ").append(this.records.size());
		for (T obj : this.records) {
			buf.append("\n").append(strIndent).append("records:");
			obj.buildString(buf, indent + 2);
		}
	}
	
	public void addSObjectListener(SObjectListener l) {
		if (this.listeners == null) {
			this.listeners = new EventListenerList();
		}
		this.listeners.add(SObjectListener.class, l);
	}
	
	public void removeSObjectListener(SObjectListener l) {
		if (this.listeners == null) {
			return;
		}
		this.listeners.remove(SObjectListener.class, l);
		if (this.listeners.getListenerCount() == 0) {
			this.listeners = null;
		}
	}
	
	private void fireEvent(SObject obj) {
		if (this.listeners == null) {
			return;
		}
		SObjectListener[] ls = (SObjectListener[])this.listeners.getListeners(SObjectListener.class);
		if (ls == null || ls.length == 0) {
			return;
		}
		SObjectEvent event = new SObjectEvent(this, obj);
		for (int i=0; i<ls.length; i++) {
			ls[i].handleEvent(event);
		}
	}
	
	//For SObject
	public static QueryResult<?> create(XMLStreamReader reader, Metadata meta) throws XMLStreamException, StAXConstructException {
		boolean done = false;
		String queryLocator = null;
		int size = 0;
		
		String targetNamespace = meta.getMessageURI();
		int event = reader.getEventType();
		while (reader.hasNext()) {
			switch (event) {
				case XMLStreamReader.START_ELEMENT:
				{
					String nsuri = reader.getNamespaceURI();
					String name = reader.getLocalName();
					if (targetNamespace.equals(nsuri)) {
						if ("done".equals(name)) {
							done = Boolean.valueOf(reader.getElementText()).booleanValue();
						} else if ("queryLocator".equals(name)) {
							queryLocator = checkNull(reader.getElementText());
						} else if ("size".equals(name)) {
							size = Integer.parseInt(reader.getElementText());
						} else if ("records".equals(name)) {
							SObject obj = new SObject(meta);
							obj.build(reader);
							if (Metadata.isClassRegistered(obj.getObjectName())) {
								SObject obj2 = meta.newObject(obj.getObjectName());
								obj2.assign(obj);
								obj = obj2;
							}
							
							QueryResult<?> ret = newInstance(meta, obj);
							ret.done = done;
							ret.queryLocator = queryLocator;
							ret.allSize = size;
							ret.addBaseObject();
							ret.build(reader, 1);
							return ret;
						}
					}
					break;
				}
			}
			event = reader.next();
		}
		throw new IllegalStateException();
	}
	
	public static <T extends SObject> QueryResult<T> newInstance(Metadata meta, T obj) {
		return new QueryResult<T>(meta, obj);
	}
	
	private static class BuildOption {
		
		public int skipRecords;
		public int maxRecords;
		
		public int skipCnt;
		
	}
}
