package org.dspace.authority.orcid.xml;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

public class OrcidNamespace implements NamespaceContext {
	private static final Map<String, String> namespaces;
	
	static {
		Map<String, String> aMap = new HashMap<String, String>();
		aMap.put("search", "http://www.orcid.org/ns/search");
		aMap.put("internal","http://www.orcid.org/ns/internal");
		aMap.put("funding","http://www.orcid.org/ns/funding");
		aMap.put("preferences","http://www.orcid.org/ns/preferences");
		aMap.put("address","http://www.orcid.org/ns/address");
		aMap.put("education","http://www.orcid.org/ns/education");
		aMap.put("work","http://www.orcid.org/ns/work");
		aMap.put("deprecated","http://www.orcid.org/ns/deprecated");
		aMap.put("other-name","http://www.orcid.org/ns/other-name");
		aMap.put("history","http://www.orcid.org/ns/history");
		aMap.put("employment","http://www.orcid.org/ns/employment");
		aMap.put("error","http://www.orcid.org/ns/error");
		aMap.put("common","http://www.orcid.org/ns/common");
		aMap.put("person","http://www.orcid.org/ns/person");
		aMap.put("activities","http://www.orcid.org/ns/activities");
		aMap.put("record","http://www.orcid.org/ns/record");
		aMap.put("researcher-url","http://www.orcid.org/ns/researcher-url");
		aMap.put("peer-review","http://www.orcid.org/ns/peer-review");
		aMap.put("personal-details","http://www.orcid.org/ns/personal-details");
		aMap.put("bulk","http://www.orcid.org/ns/bulk");
		aMap.put("keyword","http://www.orcid.org/ns/keyword");
		aMap.put("email","http://www.orcid.org/ns/email");
		aMap.put("external-identifier","http://www.orcid.org/ns/external-identifier");
		namespaces = Collections.unmodifiableMap(aMap);
	}
	
	
	@Override
	public String getNamespaceURI(String prefix) {
		return namespaces.get(prefix);
	}

	@Override
	public String getPrefix(String namespaceURI) {
		return null;
	}

	@Override
	public Iterator getPrefixes(String namespaceURI) {
		return null;
	}

}
