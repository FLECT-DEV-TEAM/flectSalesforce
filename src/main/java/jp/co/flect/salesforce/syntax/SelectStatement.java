package jp.co.flect.salesforce.syntax;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import jp.co.flect.salesforce.Metadata;
import jp.co.flect.salesforce.FieldDef;
import jp.co.flect.salesforce.RelationDef;
import jp.co.flect.salesforce.SObjectDef;

public class SelectStatement implements SelectField {
	
	private String soql;
	private List<SelectField> fieldList = new ArrayList<SelectField>();
	private List<String> nameList = null;
	
	private String from;
	private String where;
	
	public SelectStatement(String str) throws ParseException {
		StringBuilder buf = new StringBuilder();
		Tokenizer t = new Tokenizer(str);
		parse(t, buf, false);
		if (t.next(buf) != Tokenizer.T_END) {
			throw new ParseException(t.getString(), t.getPrevIndex());
		}
	}
	
	public SelectStatement(Tokenizer t) throws ParseException {
		parse(t, new StringBuilder(), false);
	}
	
	public SelectStatement(Tokenizer t, boolean subQuery) throws ParseException {
		parse(t, new StringBuilder(), subQuery);
	}
	
	private void parse(Tokenizer t, StringBuilder buf, boolean subQuery) throws ParseException {
		String str = t.getString();
		int initialIndex = t.skipWhitespace();
		int prevIndex = initialIndex;
		
		int n = t.next(buf);
		if (n != Tokenizer.T_LITERAL || !buf.toString().equalsIgnoreCase("select")) {
			throw new ParseException(str, prevIndex);
		}
		
		//Field
		boolean bEnd = false;
		while (!bEnd) {
			prevIndex = t.skipWhitespace();
			n = t.next(buf);
			SelectField field = null;
			switch (n) {
				case Tokenizer.T_LITERAL:
				{
					String name = buf.toString();
					int n2 = t.next(buf);
					if (n2 == Tokenizer.T_LITERAL && !buf.toString().equalsIgnoreCase("from")) {
						field = new SimpleField(name);
						((SimpleField)field).setAlias(buf.toString());
						prevIndex = t.skipWhitespace();
						n2 = t.next(buf);
					}
					switch (n2) {
						case Tokenizer.T_COMMA:
							if (field == null) {
								field = new SimpleField(name);
							}
							break;
						case Tokenizer.T_OPEN_BRACKET:
							//function
							SimpleField arg = null;
							int n3 = t.next(buf);
							if (n3 == Tokenizer.T_CLOSE_BRACKET) {
								//no arg. do nothing
							} else if (n3 == Tokenizer.T_LITERAL) {
								arg = new SimpleField(buf.toString());
								if (t.next(buf) != Tokenizer.T_CLOSE_BRACKET) {
									throw new ParseException(str, prevIndex);
								}
							} else {
								throw new ParseException(str, prevIndex);
							}
							field = new FunctionField(name, arg);
							
							prevIndex = t.skipWhitespace();
							n3 = t.next(buf);
							if (n3 == Tokenizer.T_LITERAL && !buf.toString().equalsIgnoreCase("from")) {
								((FunctionField)field).setAlias(buf.toString());
								prevIndex = t.skipWhitespace();
								n3 = t.next(buf);
							}
							if (n3 == Tokenizer.T_COMMA) {
								//OK
							} else if (n3 == Tokenizer.T_LITERAL && buf.toString().equalsIgnoreCase("from")) {
								bEnd = true;
							} else {
								throw new ParseException(str, prevIndex);
							}
							break;
						case Tokenizer.T_LITERAL:
							if (buf.toString().equalsIgnoreCase("from")) {
								if (field == null) {
									field = new SimpleField(name);
								}
								bEnd = true;
							} else {
								throw new ParseException(str, prevIndex);
							}
							break;
						case Tokenizer.T_ERROR:
							throw new ParseException(str, t.getIndex());
						default:
							throw new ParseException(str, prevIndex);
					}
					break;
				}
				case Tokenizer.T_OPEN_BRACKET:
				{
					field = new SelectStatement(t, true);
					prevIndex = t.skipWhitespace();
					int n2 = t.next(buf);
					if (n2 == Tokenizer.T_COMMA) {
						//Do nothing
					} else if (n2 == Tokenizer.T_LITERAL && buf.toString().equalsIgnoreCase("from")) {
						bEnd = true;
					} else {
						throw new ParseException(str, prevIndex);
					}
					break;
				}
				case Tokenizer.T_ERROR:
					throw new ParseException(str, t.getIndex());
				default:
					throw new ParseException(str, prevIndex);
			}
			if (field != null) {
				fieldList.add(field);
			}
		}
		
		//From
		prevIndex = t.skipWhitespace();
		n = t.next(buf);
		if (n != Tokenizer.T_LITERAL) {
			throw new ParseException(str, prevIndex);
		}
		this.from = buf.toString();
		
		//Where
		int whereFrom = t.skipWhitespace();
		int whereTo = checkEnd(t, buf, subQuery);
		if (whereTo > whereFrom) {
			this.where = str.substring(whereFrom, whereTo);
		}
		this.soql = str.substring(initialIndex, whereTo);
	}
	
	private int checkEnd(Tokenizer t, StringBuilder buf, boolean subQuery) throws ParseException {
		String str = t.getString();
		int prevIndex = t.getIndex();
		int n = t.next(buf);
		switch (n) {
			case Tokenizer.T_END:
				if (subQuery) {
					throw new ParseException(str, prevIndex);
				}
				return prevIndex;
			case Tokenizer.T_CLOSE_BRACKET:
				if (!subQuery) {
					throw new ParseException(str, prevIndex);
				}
				return prevIndex;
			case Tokenizer.T_LITERAL:
				int bracketCnt = 0;
				while (true) {
					prevIndex = t.getIndex();
					int n2 = t.next(buf);
					switch (n2) {
						case Tokenizer.T_END:
							if (subQuery || bracketCnt != 0) {
								throw new ParseException(str, prevIndex);
							} else {
								return t.getPrevIndex();
							}
							//break;
						case Tokenizer.T_OPEN_BRACKET:
							bracketCnt++;
							break;
						case Tokenizer.T_CLOSE_BRACKET:
							if (bracketCnt > 0) {
								bracketCnt--;
							} else if (subQuery) {
								return prevIndex;
							} else {
								throw new ParseException(str, t.getIndex());
							}
							break;
						case Tokenizer.T_ERROR:
							throw new ParseException(str, t.getIndex());
					}
				}
				//break;
			case Tokenizer.T_ERROR:
				throw new ParseException(str, t.getIndex());
			default:
				throw new ParseException(str, prevIndex);
		}
	}
	
	public int getType() { return SelectField.SUBQUERY;}
	
	public String getSoql() { return this.soql;}
	public List<SelectField> getFieldList() { return this.fieldList;}
	public String getFrom() { return this.from;}
	public String getWhere() { return this.where;}
	
	public boolean isAggregate() {
		for (SelectField f : this.fieldList) {
			if (f.getType() == SelectField.FUNCTION) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasSubquery() {
		for (SelectField f : this.fieldList) {
			if (f.getType() == SelectField.SUBQUERY) {
				return true;
			}
		}
		return false;
	}
	
	public String toString() { return this.soql;}
	
	public void normalize(Metadata meta) {
		normalize(meta, null);
	}
	
	public void normalize(Metadata meta, SObjectDef parent) {
		if (parent == null) {
			parent = meta.getObjectDef(this.from);
			if (parent != null) {
				this.from = parent.getName();
			}
		} else {
			SimpleField f = new SimpleField(this.from);
			f.normalize(meta, parent);
			this.from = f.getName();
			RelationDef target = f.getRelation(meta, parent);
			if (target != null) {
				parent = meta.getObjectDef(target.getObjectName());
			} else {
				parent = null;
			}
		}
		
		List<SelectField> expandedList = new ArrayList<SelectField>(this.fieldList.size());
		for (SelectField field : this.fieldList) {
			if (parent != null) {
				field.normalize(meta, parent);
				if (field.getType() == SelectField.SIMPLE) {
					String name = ((SimpleField)field).getName();
					if (name.equals("*") && expand(expandedList, parent, "")) {
						continue;
					} else if (name.endsWith(".*")) {
						String prefix = name.substring(0, name.length()-1);
						SObjectDef fieldParent = ((SimpleField)field).getParent(meta, parent);
						if (fieldParent != null && expand(expandedList, fieldParent, prefix)) {
							continue;
						}
					}
				}
			}
			expandedList.add(field);
		}
		if (expandedList.size() > this.fieldList.size()) {
			this.fieldList = expandedList;
			this.nameList = null;
		}
		boolean first = true;
		StringBuilder buf = new StringBuilder();
		buf.append("SELECT ");
		for (SelectField field : this.fieldList) {
			if (!first) {
				buf.append(",");
			} else {
				first = false;
			}
			if (field.getType() == SelectField.SUBQUERY) {
				buf.append("(").append(field.toString()).append(")");
			} else {
				buf.append(field.toString());
			}
		}
		buf.append(" FROM ").append(this.from);
		if (this.where != null) {
			buf.append(" ").append(this.where);
		}
		this.soql = buf.toString();
	}
	
	private boolean expand(List<SelectField> list, SObjectDef obj, String prefix) {
		List<FieldDef> fieldList = obj.getFieldList();
		if (fieldList == null || fieldList.size() == 0) {
			return false;
		}
		list.add(new SimpleField(prefix + "Id"));
		for (FieldDef f : fieldList) {
			if (!f.getName().equals("Id")) {
				list.add(new SimpleField(prefix + f.getName()));
			}
		}
		return true;
	}
	
	public List<String> getNameList() {
		if (this.nameList != null) {
			return this.nameList;
		}
		boolean aggregate = isAggregate();
		int funcIndex = 0;
		List<SelectField> list = getFieldList();
		List<String> ret = new ArrayList<String>(list.size());
		for (SelectField f : list) {
			String name = null;
			switch (f.getType()) {
				case SelectField.SIMPLE:
					SimpleField sf = (SimpleField)f;
					name = aggregate ? sf.getAggregateName() : sf.getName();
					break;
				case SelectField.FUNCTION:
					FunctionField ff = (FunctionField)f;
					if (ff.getAlias() != null) {
						name = ff.getAlias();
					} else {
						name = "expr" + (funcIndex++);
					}
					break;
				case SelectField.SUBQUERY:
					name = ((SelectStatement)f).getFrom();
					break;
				default:
					throw new IllegalStateException();
			}
			ret.add(name);
		}
		this.nameList = ret;
		return ret;
	}
	
	public List<String> getNameListWithoutSubquery() {
		if (!hasSubquery()) {
			return getNameList();
		}
		
		List<String> nameList = getNameList();
		List<String> ret = new ArrayList<String>(nameList.size());
		for (int i=0; i<nameList.size(); i++) {
			if (this.fieldList.get(i).getType() != SelectField.SUBQUERY) {
				ret.add(nameList.get(i));
			}
		}
		return ret;
	}
	
	public List<String> getSubqueryNameList(String subquery) {
		for (SelectField field : this.fieldList) {
			if (field.getType() != SelectField.SUBQUERY) {
				continue;
			}
			SelectStatement subStmt = (SelectStatement)field;
			if (subStmt.getFrom().equalsIgnoreCase(subquery)) {
				return subStmt.getNameListWithoutSubquery();
			}
		}
		return null;
	}
	
}
