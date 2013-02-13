package jp.co.flect.salesforce.io.csv;

import jp.co.flect.salesforce.io.SObjectWriter;

public abstract class CSVWriter implements SObjectWriter {
	
	/*
	private jp.co.flect.CSVWriter writer;
	
	private List<String> nameList;
	private int writeCount = 0;
	private boolean writeRecordNo = false;
	
	private Set<Integer> styledCol = null;
	private SalesforceClient client = null;
	
	private SubqueryHelper helper = new SubqueryHelper();
	
	public ExcelWriter(boolean xslx) {
		this(xslx, "Sheet1");
	}
	
	public ExcelWriter(boolean xslx, String sheetName) {
		this.workbook = xslx ? new XSSFWorkbook() : new HSSFWorkbook();
		this.sheet = workbook.createSheet(sheetName);
	}
	
	public Workbook getWorkbook() { return this.workbook;}
	public Sheet getSheet() { return this.sheet;}
	
	public int getWriteCount() { return this.writeCount;}
	
	public boolean isWriteRecordNo() { return this.writeRecordNo;}
	public void setWriteRecordNo(boolean b) { this.writeRecordNo = b;}
	
	public Point getOffset() { return this.offset;}
	public void setOffset(Point p) { this.offset = p;}
	public void setOffset(int x, int y) { this.offset = new Point(x, y);}
	
	public SalesforceClient getClient() { return this.client;}
	public void setClient(SalesforceClient client) { this.client = client;}
	
	public List<String> getNameList() { return this.nameList;}
	public void setNameList(List<String> list) { this.nameList = list;}
	
	public CellStyle getColumnStyle(int col) {
		return this.sheet.getColumnStyle(col);
	}
	
	public void setColumnStyle(int col, CellStyle style) {
		this.sheet.setDefaultColumnStyle(col, style);
		if (styledCol == null) {
			styledCol = new HashSet<Integer>();
		}
		styledCol.add(col);
	}
	
	public CellStyle getDateStyle() {
		if (this.dateStyle == null) {
			setDateStyle("yyyy-mm-dd hh:mm:ss");
		}
		return this.dateStyle;
	}
	
	public void setDateStyle(String format) {
		CellStyle style = this.workbook.createCellStyle();
		DataFormat df = this.workbook.createDataFormat();
		style.setDataFormat(df.getFormat(format));
		setDateStyle(style);
	}
	
	public void setDateStyle(CellStyle style) {
		this.dateStyle = style;
	}
	
	public CellStyle getLabelStyle() { return this.labelStyle;}
	public void setLabelStyle(CellStyle style) { this.labelStyle = style;}
	
	public void write(SObject obj) throws IOException {
		helper.clear();
		List<String> list = this.nameList;
		if (list == null) {
			list = obj.getNameList();
		}
		Row row = getRow(this.offset.y);
		int idx = 0;
		if (this.writeRecordNo) {
			Cell cell = getCell(row, this.offset.x);
			cell.setCellValue(writeCount + 1);
			idx++;
		}
		for (String name : list) {
			int col = this.offset.x + idx;
			Object value = getValue(obj, name, col);
			if (value != null && value != this.helper) {
				Cell cell = getCell(row, col);
				writeCell(cell, value);
			}
			idx++;
		}
		int rowCnt = 1;
		if (helper.hasData()) {
			rowCnt = helper.write(this.offset.y);
		}
		this.offset.y += rowCnt;
		this.writeCount++;
	}
	
	private void writeCell(Cell cell, Object value) {
		int col = cell.getColumnIndex();
		CellStyle style = null;
		if (styledCol != null && styledCol.contains(col)) {
			style = getColumnStyle(col);
		}
		if (value instanceof Date) {
			cell.setCellValue((Date)value);
			if (style == null) {
				cell.setCellStyle(getDateStyle());
			}
		} else if (value instanceof Number) {
			cell.setCellValue(((Number)value).doubleValue());
		} else if (value instanceof Boolean) {
			cell.setCellValue(((Boolean)value).booleanValue());
		} else {
			cell.setCellValue(value.toString());
		}
		if (style != null) {
			cell.setCellStyle(style);
		}
	}
	
	private Object getValue(SObject obj, String name, int col) {
		int idx = name.indexOf('.');
		if (idx == -1) {
			return obj.get(name);
		}
		String prev = name.substring(0, idx);
		String next = name.substring(idx+1);
		Object value = obj.get(prev);
		if (value instanceof SObject) {
			return getValue((SObject)value, next, col);
		} else if (value instanceof QueryResult) {
			QueryResult<?> result = (QueryResult<?>)value;
			this.helper.add(result, next, col);
			return this.helper;
		} 
		return value;
	}
	
	public void writeLabel(String... labels) {
		writeLabel(Arrays.asList(labels));
	}
	
	public void writeLabel(List<String> labels) {
		Row row = getRow(this.offset.y);
		for (int i=0; i<labels.size(); i++) {
			Cell cell = getCell(row, this.offset.x + i);
			cell.setCellValue(labels.get(i));
			if (this.labelStyle != null) {
				cell.setCellStyle(this.labelStyle);
			}
		}
		this.offset.y++;
	}
	
	public void writeTo(File f) throws IOException {
		FileOutputStream os = new FileOutputStream(f);
		try {
			writeTo(os);
		} finally {
			os.close();
		}
	}
	
	public void writeTo(OutputStream os) throws IOException {
		this.workbook.write(os);
	}
	
	private Row getRow(int row) {
		Row ret = this.sheet.getRow(row);
		if (ret== null) {
			ret = this.sheet.createRow(row);
		}
		return ret;
	}
	
	private Cell getCell(Row row, int col) {
		Cell ret = row.getCell(col);
		if (ret == null) {
			ret = row.createCell(col);
		}
		return ret;
	}
	
	private int write(int startRow, SubqueryInfo info) throws IOException {
		int ret = 0;
		QueryResult<?> result = info.result;
		for (SObject obj : result.getRecords()) {
			Row row = getRow(startRow + ret);
			for (int i=0; i<info.list.size(); i++) {
				String name = info.list.get(i).name;
				int col = info.list.get(i).col;
				Object value = getValue(obj, name, col);
				if (value != null) {
					Cell cell = getCell(row, col);
					writeCell(cell, value);
				}
			}
			ret++;
		}
		if (result.getQueryLocator() != null && this.client != null) {
			try {
				QueryResult<SObject> result2 = client.queryMore(result.getQueryLocator());
				ret += write(startRow + ret, new SubqueryInfo(result2, info.list));
			} catch (SoapException e) {
				throw new IOException(e);
			}
		}
		return ret == 0 ? 1 : ret;
	}
	
	private class SubqueryHelper {
		
		public List<SubqueryInfo> list = new ArrayList<SubqueryInfo>();
		
		public void add(QueryResult<?> result, String name, int col) {
			for (int i=0; i<list.size(); i++) {
				SubqueryInfo info = list.get(i);
				if (info.result == result) {
					info.add(name, col);
					return;
				}
			}
			list.add(new SubqueryInfo(result, name, col));
		}
		
		public void clear() {
			this.list.clear();
		}
		
		public boolean hasData() { 
			return this.list.size() > 0;
		}
		
		public int write(int startRow) throws IOException {
			int ret = 0;
			for (SubqueryInfo info : this.list) {
				int n = ExcelWriter.this.write(startRow, info);
				if (n > ret) {
					ret = n;
				}
			}
			return ret;
		}
	}
	
	private static class SubqueryInfo {
		
		public QueryResult<?> result;
		public List<ColumnInfo> list;
		
		public SubqueryInfo(QueryResult<?> result, String name, int col) {
			this.result = result;
			this.list = new ArrayList<ColumnInfo>();
			add(name, col);
		}
		
		public SubqueryInfo(QueryResult<?> result, List<ColumnInfo> list) {
			this.result = result;
			this.list = list;
		}
		
		public void add(String name, int col) {
			this.list.add(new ColumnInfo(name, col));
		}
		
	}
	
	private static class ColumnInfo {
		
		public String name;
		public int col;
		
		public ColumnInfo(String name, int col) {
			this.name = name;
			this.col = col;
		}
	}
	*/
}
