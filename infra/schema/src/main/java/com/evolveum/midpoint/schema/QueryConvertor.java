package com.evolveum.midpoint.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

public class QueryConvertor {

	public static ObjectQuery createObjectQuery(Class clazz, QueryType queryType, PrismContext prismContext)
			throws SchemaException {

		if (queryType == null){
			return null;
		}
		
		Element criteria = queryType.getFilter();
		
		if (criteria == null && queryType.getPaging() == null){
			return null;
		}
		PrismObjectDefinition objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);

		if (objDef == null) {
			throw new SchemaException("cannot find obj definition");
		}

		try {
			ObjectQuery query = new ObjectQuery();
			if (criteria != null) {
				ObjectFilter filter = parseFilter(objDef, criteria);
				query.setFilter(filter);
			}

			if (queryType.getPaging() != null) {
				ObjectPaging paging = PagingConvertor.createObjectPaging(queryType.getPaging());
				query.setPaging(paging);
			}
			return query;
		} catch (SchemaException ex) {
			throw new SchemaException("Failed to convert query. Reason: " + ex.getMessage(), ex);
		}

	}

	public static QueryType createQueryType(ObjectQuery query) throws SchemaException{

		ObjectFilter filter = query.getFilter();
		try{
		Document doc = DOMUtil.getDocument();
		Element filterType = createFilterType(filter, doc);
		QueryType queryType = new QueryType();
		queryType.setFilter(filterType);
		return queryType;
		} catch (SchemaException ex){
			throw new SchemaException("Failed to convert query. Reason: " + ex.getMessage(), ex);
		}
		

	}

	private static Element createFilterType(ObjectFilter filter, Document doc) throws SchemaException{

		if (filter instanceof AndFilter) {
			return createAndFilterType((AndFilter) filter, doc);
		}
		if (filter instanceof OrFilter) {
			return createOrFilterType((OrFilter) filter, doc);
		}
		if (filter instanceof NotFilter) {
			return createNotFilterType((NotFilter) filter, doc);
		}
		if (filter instanceof EqualsFilter) {
			return createEqualsFilterType((EqualsFilter) filter, doc);
		}
		if (filter instanceof RefFilter) {
			return createRefFilterType((RefFilter) filter, doc);
		}

		if (filter instanceof SubstringFilter) {
			return createSubstringFilterType((SubstringFilter) filter, doc);
		}

		if (filter instanceof OrgFilter) {
			return createOrgFilterType((OrgFilter) filter, doc);
		}

		throw new UnsupportedOperationException("Unsupported filter type: " + filter);
	}

	private static Element createAndFilterType(AndFilter filter, Document doc) throws SchemaException{

		Element and = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_AND);

		for (ObjectFilter of : filter.getCondition()) {
			Element element = createFilterType(of, doc);
			and.appendChild(element);
		}
		return and;
	}

	private static Element createOrFilterType(OrFilter filter, Document doc) throws SchemaException{

		Element or = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_OR);
		for (ObjectFilter of : filter.getCondition()) {
			Element element = createFilterType(of, doc);
			or.appendChild(element);
		}
		return or;
	}

	private static Element createNotFilterType(NotFilter filter, Document doc) throws SchemaException{

		Element not = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_NOT);

		Element element = createFilterType(filter.getFilter(), doc);
		not.appendChild(element);
		return not;
	}

	private static Element createEqualsFilterType(EqualsFilter filter, Document doc) throws SchemaException{

		Element equal = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_EQUAL);
		Element value = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_VALUE);
//		equal.appendChild(value);

		Element path = createPathElement(filter, doc);
		equal.appendChild(path);

		QName propertyName = filter.getDefinition().getName();
		for (PrismValue val : filter.getValues()) {
			Element propValue = DOMUtil.createElement(doc, propertyName);
			if (val instanceof PrismReferenceValue) {
				throw new SchemaException("Prism refenrence value not allowed in the equal element");
			} else {
				Object propVal = val.getPrismContext().getPrismJaxbProcessor().toAny(val, doc);
				if (propVal instanceof Element){
					value.setTextContent(((Element) propVal).getTextContent());
//					System.out.println(((Element) propVal).getTextContent());
				}
//				value.setTextContent();
				equal.appendChild(value);
//				propValue.setTextContent(String.valueOf(((PrismPropertyValue) val).getValue()));
			}
//			value.appendChild(propValue);
		}
		return equal;
	}
	
	private static Element createRefFilterType(RefFilter filter, Document doc) throws SchemaException {

		Element ref = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_REF);

		Element path = createPathElement(filter, doc);
		ref.appendChild(path);

		List<PrismReferenceValue> values = (List<PrismReferenceValue>) filter.getValues();
		if (values.size() < 1) {
			throw new SchemaException("No values for search in the ref filter.");
		}

		if (values.size() > 1) {
			throw new SchemaException("More than one prism reference value not allowed in the ref filter");
		}

		PrismReferenceValue val = values.get(0);
		if (val.getOid() != null) {
			Element oid = DOMUtil.createElement(doc, PrismConstants.Q_OID);
			oid.setTextContent(String.valueOf(val.getOid()));
			ref.appendChild(oid);
		}
		if (val.getTargetType() != null) {
			Element type = DOMUtil.createElement(doc, PrismConstants.Q_TYPE);
			XPathHolder xtype = new XPathHolder(val.getTargetType());
			type.setTextContent(xtype.getXPath());
			ref.appendChild(type);
		}
		if (val.getRelation() != null) {
			Element relation = DOMUtil.createElement(doc, PrismConstants.Q_RELATION);
			XPathHolder xrelation = new XPathHolder(val.getRelation());
			relation.setTextContent(xrelation.getXPath());
			ref.appendChild(relation);
		}

		return ref;
	}

	private static Element createSubstringFilterType(SubstringFilter filter, Document doc) {
		Element substring = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_SUBSTRING);
		Element value = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_VALUE);
		substring.appendChild(value);

		Element path = createPathElement(filter, doc);
		substring.appendChild(path);

		QName propertyName = filter.getDefinition().getName();
		String val = filter.getValue();

		Element propValue = DOMUtil.createElement(doc, propertyName);
		propValue.setTextContent(val);

		value.appendChild(propValue);

		return substring;
	}

	private static Element createOrgFilterType(OrgFilter filter, Document doc) {
		Element org = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_ORG);

		Element orgRef = null;
		if (filter.getOrgRef() != null) {
			orgRef = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_ORG_REF);
			orgRef.setAttribute("oid", filter.getOrgRef().getOid());
			org.appendChild(orgRef);
		}

		Element minDepth = null;
		if (filter.getMinDepth() != null) {
			minDepth = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_MIN_DEPTH);
			minDepth.setTextContent(filter.getMinDepth());
			org.appendChild(minDepth);
		}

		Element maxDepth = null;
		if (filter.getMaxDepth() != null) {
			maxDepth = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_MAX_DEPTH);
			maxDepth.setTextContent(filter.getMaxDepth());
			org.appendChild(maxDepth);
		}

		return org;
	}

	private static Element createPathElement(ValueFilter filter, Document doc) {
		Element path = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_PATH);
		XPathHolder xpath = null;
		if (filter.getPath() != null) {
			xpath = new XPathHolder(new PropertyPath(filter.getPath(), filter.getDefinition().getName()));
		} else {
			xpath = new XPathHolder(filter.getDefinition().getName());
		}
		path.setTextContent(xpath.getXPath());
		return path;
	}
	
	public static ObjectFilter parseFilter(PrismContainerDefinition pcd, Node filter) throws SchemaException {

		if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_AND, filter)) {
			return createAndFilter(pcd, filter);
		}

		if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_EQUAL, filter)) {
			return createEqualFilter(pcd, filter);
		}
		
		if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_EQUAL, filter)) {
			return createEqualFilter(pcd, filter);
		}
		
		if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_REF, filter)) {
			return createRefFilter(pcd, filter);
		}

		if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_SUBSTRING, filter)) {
			return createSubstringFilter(pcd, filter);
		}

		if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_ORG, filter)) {
			return createOrgFilter(pcd, filter);
		}

		if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_OR, filter)) {
			return createOrFilter(pcd, filter);
		}

		if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_NOT, filter)) {
			return createNotFilter(pcd, filter);
		}

		throw new UnsupportedOperationException("Unsupported query filter " + DOMUtil.printDom(filter));

	}

	private static AndFilter createAndFilter(PrismContainerDefinition pcd, Node filter) throws SchemaException {
		List<ObjectFilter> objectFilters = new ArrayList<ObjectFilter>();
		for (Element node : DOMUtil.listChildElements(filter)) {
			ObjectFilter objectFilter = parseFilter(pcd, node);
			objectFilters.add(objectFilter);
		}

		return AndFilter.createAnd(objectFilters);
	}

	private static OrFilter createOrFilter(PrismContainerDefinition pcd, Node filter) throws SchemaException {
		List<ObjectFilter> objectFilters = new ArrayList<ObjectFilter>();
		for (Element node : DOMUtil.listChildElements(filter)) {
			ObjectFilter objectFilter = parseFilter(pcd, node);
			objectFilters.add(objectFilter);
		}
		return OrFilter.createOr(objectFilters);
	}

	private static NotFilter createNotFilter(PrismContainerDefinition pcd, Node filter) throws SchemaException {
//		NodeList filters = filter.getChildNodes();
		List<Element> filters = DOMUtil.listChildElements(filter);

		if (filters.size() < 1) {
			throw new SchemaException("NOT filter does not contain any values specified");
		}

		if (filters.size() > 1) {
			throw new SchemaException(
					"NOT filter can have only one value specified. For more value use OR/AND filter as a parent.");
		}

		ObjectFilter objectFilter = parseFilter(pcd, filters.get(0));
		return NotFilter.createNot(objectFilter);
	}

	private static EqualsFilter createEqualFilter(PrismContainerDefinition pcd, Node filter) throws SchemaException {
		
		PropertyPath path = getPath((Element) filter);

		if (path == null || path.isEmpty()){
		throw new SchemaException("Could not convert query, because query does not contain property path.");	
		}
		
		List<Element> values = getValues(filter);
		
		if (values == null || values.isEmpty()){
			Element expression = DOMUtil.findElementRecursive((Element) filter, SchemaConstantsGenerated.C_EXPRESSION);
			if (expression == null){
				expression = DOMUtil.findElementRecursive((Element) filter, SchemaConstantsGenerated.C_VALUE_EXPRESSION);
			}
			ItemDefinition itemDef = pcd.findItemDefinition(path);
			return EqualsFilter.createEqual(path.allExceptLast(), itemDef, expression);
		}
		
		if (path.last() == null){
			throw new SchemaException("Cannot convert query, becasue query does not contian property path.");
		}
		QName propertyName = path.last().getName();
		path = path.allExceptLast();
		if (path.isEmpty()){
			path = null;
		}
		
		Item item = getItem(values, pcd, path, propertyName, false);
		ItemDefinition itemDef = item.getDefinition();
		if (itemDef == null) {
			throw new SchemaException("Item definition for property " + item.getName() + " in container definition " + pcd
					+ " not found.");
		}

		
		if (item.getValues().size() < 1 ) {
			throw new IllegalStateException("No values to search specified for item " + itemDef);
		}

		if (itemDef.isSingleValue()) {
			if (item.getValues().size() > 1) {
				throw new IllegalStateException("Single value property "+itemDef.getName()+"should have specified only one value.");
			}
		}
		return EqualsFilter.createEqual(path, itemDef, item.getValues());
	}
	
	private static RefFilter createRefFilter(PrismContainerDefinition pcd, Node filter) throws SchemaException{
		PropertyPath path = getPath((Element) filter);
		
		if (path == null || path.isEmpty()){
			throw new SchemaException("Cannot convert query, becasue query does not contian property path.");
		}
		
		List<Element> values = DOMUtil.listChildElements(filter);
		
		if (path.last() == null){
			throw new SchemaException("Cannot convert query, becasue query does not contian property path.");
		}
		
		QName propertyName = path.last().getName();
		path = path.allExceptLast();
		if (path.isEmpty()){
			path = null;
		}
		
		Item item = getItem(values, pcd, path, propertyName, true);
		ItemDefinition itemDef = item.getDefinition();
		if (itemDef == null) {
			throw new SchemaException("Item definition for property " + item.getName() + " in container definition " + pcd
					+ " not found.");
		}

		Element expression = DOMUtil.findElementRecursive((Element) filter, SchemaConstantsGenerated.C_EXPRESSION);
		
		if (item.getValues().size() < 1 && expression == null) {
			throw new IllegalStateException("No values to search specified for item " + itemDef);
		}

		if (expression != null) {
			return RefFilter.createReferenceEqual(path, itemDef, expression);
		} 
		return RefFilter.createReferenceEqual(path, itemDef, item.getValues());
	}

	private static Item getItem(List<Element> values, PrismContainerDefinition pcd,
			PropertyPath path, QName propertyName, boolean reference) throws SchemaException {
		
		if (propertyName ==  null){
			throw new SchemaException("No property name in the search query specified.");
		}

		if (path != null) {
			pcd = pcd.findContainerDefinition(path);
		}
		Collection<Item> items = pcd.getPrismContext().getPrismDomProcessor().parseContainerItems(pcd, values, propertyName, reference);

		if (items.size() > 1) {
			throw new SchemaException("Expected presence of a single item (path " + path
					+ ") in a object modification, but found " + items.size() + " instead");
		}
		if (items.size() < 1) {
			throw new SchemaException("Expected presence of a value (path " + path
					+ ") in a object modification, but found nothing");
		}
		return  items.iterator().next();
		
	}

	private static PropertyPath getPath(Element filter) {
		Element path = DOMUtil.getChildElement((Element) filter, SchemaConstantsGenerated.Q_PATH);
		XPathHolder xpath = new XPathHolder((Element) path);
		return xpath.toPropertyPath();
	}

	private static List<Element> getValues(Node filter) {
		return DOMUtil.getElement((Element) filter, SchemaConstantsGenerated.Q_VALUE);
//		return DOMUtil.listChildElements(value);
	}

	private static SubstringFilter createSubstringFilter(PrismContainerDefinition pcd, Node filter)
			throws SchemaException {

		PropertyPath path = getPath((Element) filter);
		if (path == null || path.isEmpty()){
			throw new SchemaException("Cannot convert query, becasue query does not contian property path.");
		}
		List<Element> values = getValues(filter);

		if (path.last() == null){
			throw new SchemaException("Cannot convert query, becasue query does not contian property path.");
		}
		QName propertyName = path.last().getName();
		path = path.allExceptLast();
		if (path.isEmpty()){
			path = null;
		}
		
		if (values.size() > 1) {
			throw new SchemaException("More than one value specified in substring filter.");
		}

		if (values.size() < 1) {
			throw new SchemaException("No value specified in substring filter.");
		}

		Item item = getItem(values, pcd, path, propertyName, false);
		ItemDefinition itemDef = item.getDefinition();

		String substring = values.get(0).getTextContent();

		return SubstringFilter.createSubstring(path, itemDef, substring);
	}

	private static OrgFilter createOrgFilter(PrismContainerDefinition pcd, Node filter) throws SchemaException {

		Element orgRef = DOMUtil.getChildElement((Element) filter, SchemaConstantsGenerated.Q_ORG_REF);

		if (orgRef == null) {
			throw new SchemaException("No organization refenrence defined in the search query.");
		}

		String orgOid = orgRef.getAttribute("oid");

		if (orgOid == null || StringUtils.isBlank(orgOid)) {
			throw new SchemaException("No oid attribute defined in the organization reference element.");
		}

		Element minDepth = DOMUtil.getChildElement((Element) filter, SchemaConstantsGenerated.Q_MIN_DEPTH);

		String min = null;
		if (minDepth != null) {
			min = minDepth.getTextContent();
		}

		Element maxDepth = DOMUtil.getChildElement((Element) filter, SchemaConstantsGenerated.Q_MAX_DEPTH);
		String max = null;
		if (maxDepth != null) {
			max = maxDepth.getTextContent();
		}

		return OrgFilter.createOrg(orgOid, min, max);
	}

}
