package jp.co.flect.salesforce.bulk;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;

import jp.co.flect.salesforce.SObject;
import jp.co.flect.io.FileUtils;
import jp.co.flect.csv.CSVWriter;


public class AttachmentZipBuilder {
	
	private File baseDir;
	private List<String> nameList;
	private CSVWriter writer;
	
	public AttachmentZipBuilder(File baseDir, List<String> nameList) throws IOException {
		if (!baseDir.exists()) {
			baseDir.mkdirs();
		}
		if (!baseDir.exists() || !baseDir.isDirectory()) {
			throw new IllegalArgumentException(baseDir.toString());
		}
		this.baseDir = baseDir;
		this.nameList = nameList;
		
		this.writer = new CSVWriter(new File(baseDir, "request.txt"));
		write((String[])nameList.toArray(new String[nameList.size()]));
	}
	
	public void add(SObject obj, File file) throws IOException {
		if (!file.exists() || !file.isFile()) {
			throw new IllegalArgumentException(file.toString());
		}
		String suffix = FileUtils.getExt(file);
		File tempFile = File.createTempFile("tmp", suffix == null ? "" : "." + suffix, baseDir);
		FileUtils.copy(file, tempFile);
		
		String[] lines = new String[nameList.size()];
		for (int i=0; i<nameList.size(); i++) {
			String name = nameList.get(i);
			if (name.equalsIgnoreCase("name")) {
				lines[i] = file.getName();
			} else if (name.equalsIgnoreCase("body")) {
				lines[i] = "#" + tempFile.getName();
			} else {
				lines[i] = obj.getString(name);
			}
		}
		write(lines);
	}
	
	private void write(String[] lines) throws IOException {
		if (this.writer == null) {
			this.writer = new CSVWriter(new FileOutputStream(new File(baseDir, "request.txt"), true));
		}
		this.writer.write(lines);
	}
	
	public void writeTo(File file) throws IOException {
		writeTo(new FileOutputStream(file));
	}
	
	public void writeTo(OutputStream os) throws IOException {
		if (this.writer != null) {
			this.writer.close();
			this.writer = null;
		}
		ZipOutputStream zos = new ZipOutputStream(os);
		try {
			byte[] buf = new byte[8192];
			
			File[] files = baseDir.listFiles();
			for (int i=0; i<files.length; i++) {
				FileInputStream is = new FileInputStream(files[i]);
				try {
					zos.putNextEntry(new ZipEntry(files[i].getName()));
					int n = is.read(buf);
					while (n != -1) {
						zos.write(buf, 0, n);
						n = is.read(buf);
					}
					zos.closeEntry();
				} finally {
					is.close();
				}
			}
		} finally {
			zos.close();
		}
	}
	
	public void deleteAll() {
		FileUtils.deleteRecursive(this.baseDir);
	}
}
