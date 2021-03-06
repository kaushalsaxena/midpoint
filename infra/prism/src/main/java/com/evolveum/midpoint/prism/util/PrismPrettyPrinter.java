/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 * @author mederly
 */
public class PrismPrettyPrinter {

	private static final Trace LOGGER = TraceManager.getTrace(PrismPrettyPrinter.class);
	private static final String CRLF_REGEX = "(\\r|\\n|\\r\\n)+";
	private static final Pattern CRLF_PATTERN = Pattern.compile(CRLF_REGEX);

	public static String prettyPrint(RawType raw) {
		if (raw.getAlreadyParsedValue() != null) {
			return PrettyPrinter.prettyPrint(raw.getAlreadyParsedValue());
		}
		if (raw.getXnode() != null && raw.getPrismContext() != null) {
			try {
				String jsonText = raw.getPrismContext().jsonSerializer().serialize(raw.getRootXNode(new QName("value")));
				return CRLF_PATTERN.matcher(jsonText).replaceAll("");
			} catch (Throwable t) {
				LoggingUtils.logException(LOGGER, "Couldn't serialize raw value for pretty printing, using 'toString' instead: {}", t, raw.getXnode());
			}
		}
		return PrettyPrinter.prettyPrint(raw.getXnode());
	}

	// TODO deduplicate this with prettyPrintForReport in ReportUtils
	public static String prettyPrint(PrismPropertyValue<?> ppv) {
		String retPPV;
		try {
			retPPV = PrettyPrinter.prettyPrint(ppv.getValue());
		} catch (Throwable t) {
			return "N/A"; // rare case e.g. for password-type in resource
		}
		return retPPV;
	}

	public static String prettyPrint(PrismContainerValue<?> pcv) {
		return pcv.getItems().stream()
				.map(item -> PrettyPrinter.prettyPrint(item))
				.collect(Collectors.joining(", "));
	}

	public static String prettyPrint(Item<?, ?> item) {
		String values = item.getValues().stream()
				.map(value -> PrettyPrinter.prettyPrint(value))
				.collect(Collectors.joining(", "));
		return PrettyPrinter.prettyPrint(item.getElementName()) + "={" + values + "}";
	}

	public static String prettyPrint(PrismReferenceValue prv) {
		return prettyPrint(prv, true);
	}

	public static String prettyPrint(PrismReferenceValue prv, boolean showType) {
		StringBuilder sb = new StringBuilder();
		if (showType) {
			sb.append(PrettyPrinter.prettyPrint(prv.getTargetType()));
			sb.append(": ");
		}
		if (prv.getTargetName() != null) {
			sb.append(prv.getTargetName());
		} else {
			sb.append(prv.getOid());
		}
		return sb.toString();
	}

	static {
		PrettyPrinter.registerPrettyPrinter(PrismPrettyPrinter.class);
	}

	public static void initialize() {
		// nothing to do here, we just make sure static initialization will take place
	}

	public static String debugDumpValue(int indent, Object value, PrismContext prismContext, QName elementName, String defaultLanguage) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		debugDumpValue(sb, indent, value, prismContext, elementName, defaultLanguage);
		return sb.toString();
	}

	// TODO a better place? cannot be in DebugUtil, because of the missing dependency on prismContext
	// Note that expectedIndent applies only to lines after the first one. The caller is responsible for preparing
	// indentation for the first line.
	public static void debugDumpValue(StringBuilder sb, int expectedIndent, Object value, PrismContext prismContext, QName elementName, String defaultLanguage) {
		String formatted;
		String language = DebugUtil.getPrettyPrintBeansAs() != null ? DebugUtil.getPrettyPrintBeansAs() : defaultLanguage;
		if (elementName == null) {
			elementName = new QName("value");
		}
		if (language != null && value != null && !(value instanceof Enum) && prismContext != null
				&& value.getClass().getAnnotation(XmlType.class) != null) {
			try {
				formatted = prismContext.serializerFor(language).serializeRealValue(value, elementName);
			} catch (SchemaException e) {
				formatted = PrettyPrinter.prettyPrint(value);
			}
		} else {
			formatted = PrettyPrinter.prettyPrint(value);
		}
		sb.append(DebugUtil.fixIndentInMultiline(expectedIndent, DebugDumpable.INDENT_STRING, formatted));
	}
}
