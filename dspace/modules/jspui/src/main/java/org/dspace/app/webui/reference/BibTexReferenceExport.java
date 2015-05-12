package org.dspace.app.webui.reference;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.BibTeXFormatter;
import org.jbibtex.Key;
import org.jbibtex.StringValue;
import org.jbibtex.Value;

public class BibTexReferenceExport extends ReferenceExport {
	ItemIterator toExport;
	private static Logger log = Logger.getLogger(BibTexReferenceExport.class);
	
	public BibTexReferenceExport(Context context, DSpaceObject object) {
		try {
			toExport = getItemIterator(context, object);
		}
		catch (SQLException e) {
			log.error("Error retrieving item iterator");
		}
	}
	
	public String getContentType() {
		return "application/x-bibtex";
	}
	
	public String getPostfix() {
		return "bib";
	}
	
	public void export(Writer writer) throws IOException {
		BibTeXDatabase database = new BibTeXDatabase();
		// if (exportAll || okToExport(value))
		try {
			while (toExport.hasNext()) {
				Item item = toExport.next();
				BibTeXEntry entry = getBibTeXEntry(item);
				if (entry != null) {
					database.addObject(entry);
				}
				//item.
				//BibTeXEntry entry = new BibTexEntry();
			}
		}
		catch (Exception e) {
			log.error("Error exporting to BibTex", e);
		}
		BibTeXFormatter formatter = new BibTeXFormatter();
		formatter.format(database, writer);
		//LaTeXPrinter printer = new LaTexPrinter();
		//printer.print(database.getObjects());
		//writer.write(arg0);
	}

	@SuppressWarnings("deprecation")
	public BibTeXEntry getBibTeXEntry(Item item) {
		Properties properties = ConfigurationManager.getProperties("bibtex-export");
		String typeField = properties.getProperty("bibtex.type.field");
		DCValue[] types = item.getMetadata(typeField);
		if (types == null || types.length == 0) {
			return null;
		}
		String type = "bibtex.type." + types[0].value;
		String bibtexType = properties.getProperty(type);
		if (bibtexType == null || "".equals(bibtexType)) {
			log.info("No BibTeX type defined for "+type);
			return null;
		}
		String bibtexFields = properties.getProperty("bibtex."+bibtexType+".fields");
		String[] bibtexFieldsArray = bibtexFields.split(", ");
		//TODO fix key
		BibTeXEntry entry = null;

		String key = item.getHandle().replace("/", "-");
		//String key = UUID.randomUUID().toString();
		entry = new BibTeXEntry(new Key(bibtexType),new Key(key));
		//ConfigurationManager.getProperty("bibtex-export", property)
		for (String bibtexField : bibtexFieldsArray) {
			addEntryField(properties, bibtexField, item, entry);
		}
		
		return entry;
	}

	@SuppressWarnings("deprecation")
	private void addEntryField(Properties properties, String field, Item item, BibTeXEntry entry) {
		String dspaceFields = properties.getProperty("bibtex.metadata."+field);
		if (dspaceFields == null || "".equals(dspaceFields)) {
			//TODO remove!
			log.info("No values defined for: "+field);
			return;
		}
		String[] dspaceFieldArray = dspaceFields.split(", ");
		StringBuilder bibtexFieldValue = new StringBuilder();
		boolean firstValue = true;
		for (String dspaceField : dspaceFieldArray) {
			DCValue[] values = item.getMetadata(dspaceField);
			for (DCValue value : values) {
				if (!firstValue) {
					bibtexFieldValue.append(" and ");
				}
				firstValue = false;
				//TODO affliation
				if ("year".equals(field)) {
					String year = processYear(value.value);
					if (year != null && year.length() > 0) {
						bibtexFieldValue.append(year);
					}
				}
				else if ("month".equals(field)) {
					String month = processMonth(value.value);
					if (month != null && month.length() > 0) {
						bibtexFieldValue.append(month);
					}
				}
				else if ("howpublished".equals(field) && value.value.startsWith("10.")) {
					bibtexFieldValue.append(resolveDOI(value.value));
				}
				else {
					bibtexFieldValue.append(value.value);
				}
			}
		}
		if (bibtexFieldValue.length() > 0) {
			String fieldValue = bibtexFieldValue.toString();
			if ("pages".equals(field)) {
				fieldValue = fieldValue.replaceAll(" and ", "-");
			}
			fieldValue = format(fieldValue);
			Value bibTexValue = new StringValue(fieldValue, StringValue.Style.BRACED);
			entry.addField(new Key(field), bibTexValue);
		}
	}
	
	private String processYear(String value) {
		if (value != null && !"".equals(value)) {
			return value.substring(0, 4);
		}
		return null;
	}
	
	private String processMonth(String value) {
		if (value != null && value.length() >= 7) {
			log.info("Value for month: "+value);
			log.info("Pre-parse Month: "+value.substring(5,7));
			int month = Integer.parseInt(value.substring(5,7));
			log.info("Post-parse Month: "+month);
			if (month > 0) {
				return Integer.toString(month);
			}
		}
		return null;
	}
	
	private String resolveDOI(String value) {
		return "http://dx.doi.org/"+value;
	}
	
	private String format(String formatValue) {
		//StringUtils.
		//Normalizer.no

		for (Entry<String, String> entry : accentedCharacters.entrySet()) {
			formatValue = formatValue.replace(entry.getKey(), entry.getValue());
		}
		
		return formatValue;
	}

	private static Map<String, String> accentedCharacters = createMap();
	
	private static Map<String, String> createMap() {
		Map<String, String> result = new HashMap<String, String>();
		result.put("\u00c0", "{\\`A}");
		result.put("\u00c8", "{\\`E}");
		result.put("\u00cc", "{\\`I}");
		result.put("\u00d2", "{\\`O}");
		result.put("\u00d9", "{\\`U}");
		
		result.put("\u00e0", "{\\`a}");
		result.put("\u00e8", "{\\`e}");
		result.put("\u00ec", "{\\`i}");
		result.put("\u00f2", "{\\`o}");
		result.put("\u00f9", "{\\`u}");
		
		result.put("\u00c1", "{\\\'A}");
		result.put("\u00c9", "{\\\'E}");
		result.put("\u00cd", "{\\\'I}");
		result.put("\u00d3", "{\\\'O}");
		result.put("\u00da", "{\\\'U}");
		result.put("\u00dd", "{\\\'Y}");
		
		result.put("\u00e1", "{\\\'a}");
		result.put("\u00e9", "{\\\'e}");
		result.put("\u00ed", "{\\\'i}");
		result.put("\u00f3", "{\\\'o}");
		result.put("\u00fa", "{\\\'u}");
		result.put("\u00fd", "{\\\'y}");
		
		result.put("\u00c4", "{\\\"A}");
		result.put("\u00cb", "{\\\"E}");
		result.put("\u00cf", "{\\\"I}");
		result.put("\u00d6", "{\\\"O}");
		result.put("\u00dc", "{\\\"U}");
		
		result.put("\u00e4", "{\\\"a}");
		result.put("\u00eb", "{\\\"e}");
		result.put("\u00ef", "{\\\"i}");
		result.put("\u00f6", "{\\\"o}");
		result.put("\u00fc", "{\\\"u}");
		
		result.put("\u00c3", "{\\~A}");
		result.put("\u00d1", "{\\~N}");
		result.put("\u00d5", "{\\~O}");
		
		result.put("\u00e3", "{\\~a}");
		result.put("\u00f1", "{\\~n}");
		result.put("\u00f5", "{\\~o}");
		
		result.put("\u010c", "{\\v C");
		result.put("\u010e", "{\\v D");
		result.put("\u011a", "{\\v E");
		result.put("\u0147", "{\\v N");
		result.put("\u0158", "{\\v R");
		result.put("\u0160", "{\\v S");
		result.put("\u0164", "{\\v T");
		result.put("\u017d", "{\\v Z");
		
		result.put("\u010d", "{\\v c");
		result.put("\u010f", "{\\v d");
		result.put("\u011b", "{\\v e");
		result.put("\u0148", "{\\v n");
		result.put("\u0159", "{\\v r");
		result.put("\u0161", "{\\v s");
		result.put("\u0165", "{\\v t");
		result.put("\u017e", "{\\v z");
		
		return Collections.unmodifiableMap(result);
	};
}
