package org.dspace.submit.lookup;

import gr.ekt.bte.core.AbstractModifier;
import gr.ekt.bte.core.MutableRecord;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.StringValue;
import gr.ekt.bte.core.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * Sets the fields values to lowercase.
 * 
 * @author Genevieve Turner
 *
 */
public class LowercaseModifier extends AbstractModifier {
	private String field;
	
	public LowercaseModifier() {
		super("LowercaseModifier");
	}

	@Override
	public Record modify(MutableRecord rec) {
		List<Value> values = rec.getValues(field);
		if (values != null) {
			List<Value> converted_values = new ArrayList<Value>();
			for (Value val : values) {
				converted_values.add(new StringValue(val.getAsString().toLowerCase()));
			}
			rec.updateField(field, converted_values);
		}
		return rec;
	}
	
	/**
	 * Get the field name
	 * 
	 * @return The field
	 */
	public String getField() {
		return field;
	}
	
	/**
	 * Set the field name
	 * 
	 * @param field The field to set
	 */
	public void setField(String field) {
		this.field = field;
	}

}
