package jp.co.flect.salesforce.metadata;

import jp.co.flect.soap.SimpleObject;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import jp.co.flect.xml.StAXConstructException;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.codec.binary.Base64OutputStream;

/*
   <xsd:complexType name="RetrieveResult">
    <xsd:sequence>
     <xsd:element name="fileProperties" minOccurs="0" maxOccurs="unbounded" type="tns:FileProperties"/>
     <xsd:element name="id" type="xsd:string"/>
     <xsd:element name="messages" minOccurs="0" maxOccurs="unbounded" type="tns:RetrieveMessage"/>
     <xsd:element name="zipFile" type="xsd:base64Binary"/>
    </xsd:sequence>
   </xsd:complexType>
*/
public class RetrieveResult extends SimpleObject {
	
	/**
	<xsd:complexType name="RetrieveMessage">
		<xsd:sequence>
			<xsd:element name="fileName" type="xsd:string"/>
			<xsd:element name="problem" type="xsd:string"/>
		</xsd:sequence>
	</xsd:complexType>
	*/
	public static class RetrieveMessage extends SimpleObject {
		
		public String getFileName() { return getString("fileName");}
		public String getProblem() { return getString("problem");}
		
	}
	
	private OutputStream os;
	
	public RetrieveResult(OutputStream os) {
		this.os = new Base64OutputStream(os, false);
	}
	
	public String getId() { return getString("id");}
	
	public List<FileProperties> getFileProperties() { return (List<FileProperties>)get("fileProperties");}
	public void setFileProperties(List<FileProperties> list) { set("fileProperties", list);}
	
	public void addFileProperties(FileProperties fp) {
		List<FileProperties> list = getFileProperties();
		if (list == null) {
			list = new ArrayList<FileProperties>();
			setFileProperties(list);
		}
		list.add(fp);
	}
	
	public List<RetrieveMessage> getMessages() { return (List<RetrieveMessage>)get("messages");}
	public void setMessages(List<RetrieveMessage> list) { set("messages", list);}
	
	public void addMessages(RetrieveMessage msg) {
		List<RetrieveMessage> list = getMessages();
		if (list == null) {
			list = new ArrayList<RetrieveMessage>();
			setMessages(list);
		}
		list.add(msg);
	}
	
	@Override
	protected boolean startElement(XMLStreamReader reader) throws XMLStreamException, StAXConstructException {
		String name = reader.getLocalName();
		if (name.equals("fileProperties")) {
			FileProperties fp = new FileProperties();
			fp.build(reader);
			addFileProperties(fp);
			return true;
		} else if (name.equals("messages")) {
			RetrieveMessage msg = new RetrieveMessage();
			msg.build(reader);
			addMessages(msg);
			return true;
		} else if (name.equals("zipFile")) {
			try {
				int n = reader.next();
				while (n == XMLStreamReader.CHARACTERS || n == XMLStreamReader.CDATA) {
					String text = reader.getText();
					if (text != null) {
						os.write(text.getBytes("us-ascii"));
					}
					n = reader.next();
				}
			} catch (IOException e) {
				throw new StAXConstructException(e);
			}
			return true;
		} else {
			return super.startElement(reader);
		}
	}
}

