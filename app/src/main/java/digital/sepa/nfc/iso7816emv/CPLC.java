/*
 * Copyright 2010 sasc
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
package digital.sepa.nfc.iso7816emv;

import digital.sepa.nfc.exceptions.TlvParsingException;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static digital.sepa.nfc.iso7816emv.EmvUtils.calculateCplcDate;
import static digital.sepa.nfc.iso7816emv.EmvUtils.getNextTLV;
import static digital.sepa.nfc.util.Utils.*;

/**
 * Card Production Life-Cycle Data (CPLC) as defined by the Global Platform Card
 * Specification (GPCS)
 * 
 * Provides information on "who did what" prior to card issuance.
 *
 * Based on code by nelenkov
 */
public class CPLC {

	private static final Map<String, Integer> FIELD_NAMES_LENGTHS = new LinkedHashMap<String, Integer>();
	private Map<String, String> fields = new LinkedHashMap<String, String>();

	private static final String FIELD_NAME_IC_FABRICATOR = "IC Fabricator";
	private static final String FIELD_NAME_IC_TYPE = "IC Type";
	private static final String FIELD_NAME_OPERATING_SYSTEM = "Operating System";
	private static final String FIELD_NAME_OPERATING_SYSTEM_REL_DATE = "Operating System Release Date";
	private static final String FIELD_NAME_OPERATING_SYSTEM_REL_LEVEL = "Operating System Release Level";
	private static final String FIELD_NAME_IC_FABRIC_DATE = "IC Fabrication Date";
	private static final String FIELD_NAME_IC_SERIAL_NUMBER = "IC Serial Number";
	private static final String FIELD_NAME_IC_BATCH_ID = "IC Batch Identifier";
	private static final String FIELD_NAME_IC_MODULE_FABRICATOR = "IC ModuleFabricator";
	private static final String FIELD_NAME_IC_PACKAGING_DATE = "IC ModulePackaging Date";
	private static final String FIELD_NAME_ICC_MANUFACTURER = "ICC Manufacturer";
	private static final String FIELD_NAME_IC_EMBEDDING_DATE = "IC Embedding Date";
	private static final String FIELD_NAME_PREPERSO_ID = "Prepersonalizer Identifier";
	private static final String FIELD_NAME_PREPERSO_DATE = "Prepersonalization Date";
	private static final String FIELD_NAME_PREPERSO_EQUIPMENT = "Prepersonalization Equipment";
	private static final String FIELD_NAME_PERSO_ID = "Personalizer Identifier";
	private static final String FIELD_NAME_PERSO_DATE = "Personalization Date";
	private static final String FIELD_NAME_PERSO_EQUIPMENT = "Personalization Equipment";

	static {
		FIELD_NAMES_LENGTHS.put(FIELD_NAME_IC_FABRICATOR, 2);
		FIELD_NAMES_LENGTHS.put(FIELD_NAME_IC_TYPE, 2);
		FIELD_NAMES_LENGTHS.put(FIELD_NAME_OPERATING_SYSTEM, 2);
		FIELD_NAMES_LENGTHS.put(FIELD_NAME_OPERATING_SYSTEM_REL_DATE, 2);
		FIELD_NAMES_LENGTHS.put(FIELD_NAME_OPERATING_SYSTEM_REL_LEVEL, 2);
		FIELD_NAMES_LENGTHS.put(FIELD_NAME_IC_FABRIC_DATE, 2);
		FIELD_NAMES_LENGTHS.put(FIELD_NAME_IC_SERIAL_NUMBER, 4);
		FIELD_NAMES_LENGTHS.put(FIELD_NAME_IC_BATCH_ID, 2);
		FIELD_NAMES_LENGTHS.put(FIELD_NAME_IC_MODULE_FABRICATOR, 2);
		FIELD_NAMES_LENGTHS.put(FIELD_NAME_IC_PACKAGING_DATE, 2);
		FIELD_NAMES_LENGTHS.put(FIELD_NAME_ICC_MANUFACTURER, 2);
		FIELD_NAMES_LENGTHS.put(FIELD_NAME_IC_EMBEDDING_DATE, 2);
		FIELD_NAMES_LENGTHS.put(FIELD_NAME_PREPERSO_ID, 2);
		FIELD_NAMES_LENGTHS.put(FIELD_NAME_PREPERSO_DATE, 2);
		FIELD_NAMES_LENGTHS.put(FIELD_NAME_PREPERSO_EQUIPMENT, 4);
		FIELD_NAMES_LENGTHS.put(FIELD_NAME_PERSO_ID, 2);
		FIELD_NAMES_LENGTHS.put(FIELD_NAME_PERSO_DATE, 2);
		FIELD_NAMES_LENGTHS.put(FIELD_NAME_PERSO_EQUIPMENT, 4);
	}

	private CPLC() {
	}

	public static CPLC parse(byte[] raw) throws TlvParsingException {
		CPLC result = new CPLC();

		byte[] cplc = null;
		// try to interpret as raw data (not TLV)
		if (raw.length == 42) {
			cplc = raw;
		}
		// or maybe it's prepended with CPLC tag:
		else if (raw.length == 45) {
			BERTLV tlv = getNextTLV(new ByteArrayInputStream(raw));
			if (!tlv.getTag().equals(GPTags.CPLC)) {
				throw new IllegalArgumentException(
						"CPLC data not valid. Found tag: " + tlv.getTag());
			}
			cplc = tlv.getValueBytes();
		} else {
			throw new IllegalArgumentException("CPLC data not valid.");
		}
		int idx = 0;

		for (String fieldName : FIELD_NAMES_LENGTHS.keySet()) {
			int length = FIELD_NAMES_LENGTHS.get(fieldName);
			byte[] value = Arrays.copyOfRange(cplc, idx, idx + length);
			idx += length;
			String valueStr = bytesToHex(value);
			result.fields.put(fieldName, valueStr);
		}
		return result;
	}

	/**
	 * Global Platform CUID
	 * 
	 * Concatenating four data fields from the Global Platform Card Production
	 * Life Cycle (CPLC) data in the following sequence forms a card unique
	 * identifier (CUID): ICFabricatorID || ICType || ICBatchIdentifier ||
	 * ICSerialNumber (10 bytes)
	 * 
	 * @return
	 */
	public String createCardUniqueIdentifier() {
		return fields.get(FIELD_NAME_IC_FABRICATOR)
				+ fields.get(FIELD_NAME_IC_TYPE)
				+ fields.get(FIELD_NAME_IC_BATCH_ID)
				+ fields.get(FIELD_NAME_IC_SERIAL_NUMBER);
	}

	@Override
	public String toString() {
		StringWriter sw = new StringWriter();
		dump(new PrintWriter(sw), 0);
		return sw.toString();
	}

	/**
	 * Prints information about this CPLC
	 * 
	 * @param pw
	 * @param indent
	 */
	public void dump(PrintWriter pw, int indent) {
		pw.println("Card Production Life Cycle Data (CPLC)");
		for (String key : fields.keySet()) {
			pw.println(String.format("%s: %s", key, fields.get(key)
					+ (FIELD_NAME_IC_FABRICATOR.equals(key) ? " ("
							+ getFabricatorName(fields.get(key)) + ")" : "")));
		}
		pw.println(" -> Card Unique Identifier: "
				+ createCardUniqueIdentifier());
	}

	public static String getFabricatorName(String id) {
		if ("4180".equals(id)) {
			return "Atmel";
		}
		if ("4250".equals(id)) {
			return "Samsung";
		}
		if ("4790".equals(id)) {
			return "NXP";
		}
		if ("4090".equals(id)) {
			return "Infineon Technologies AG";
		}
		if ("2391".equals(id)) {
			return "AUSTRIA CARD";
		}
		if ("3060".equals(id)) {
			return "Renesas";
		}
		// seen on an Austrian Mastercard from Kalixa
		if ("1180".equals(id)) {
			return "cpi-pf (CPI Card Group)";
		}
		// seen on a Romanian Mastercard
		if ("1143".equals(id)) {
			return "Oberthur Technologies";
		}
		return "Unknown (0x" + id + ")";
	}

	public static String getIcTypeName(String id) {
		if ("5032".equals(id)) {
			return "SmartMX";
		}
		return "Unknown (0x" + id + ")";
	}

	public static String getOperatingSystemprovider(String id) {
		if ("2391".equals(id)) {
			return "AUSTRIA CARD OS (ACOS)";
		}
		if ("8211".equals(id)) {
			return "SCS OS";
		}
		if ("1291".equals(id) || "1981".equals(id)) {
			return "TOP";
		}
		if ("230".equals(id) || "0230".equals(id)) {
			return "G230";
		}
		if ("D000".equalsIgnoreCase(id)) {
			return "Gemalto OS";
		}
		if ("4051".equals(id) || "4A5A".equalsIgnoreCase(id)
				|| "4070".equals(id) || "4791".equals(id)) {
			return "NXP JCOP";
		}
		if ("4091".equals(id)) {
			return "Trusted Logic jTOP";
		}
		if ("8231".equals(id)) {
			return "OCS";
		}
		if ("1671".equals(id)) {
			return "G&D Sm@rtCaf";
		}
		if ("27".equals(id) || "027".equals(id) || "0027".equals(id)) {
			return "STM027";
		}
		return "Unknown (0x" + id + ")";
	}

	public static String getHumanReadableValue(final String key,
                                               final String val) {
		if (FIELD_NAME_IC_FABRICATOR.equals(key)) {
			return getFabricatorName(val);
		}
		if (FIELD_NAME_ICC_MANUFACTURER.equals(key)) {
			return getFabricatorName(val);
		}
		if (FIELD_NAME_IC_MODULE_FABRICATOR.equals(key)) {
			return getFabricatorName(val);
		}
		if (FIELD_NAME_PREPERSO_ID.equals(key)) {
			return getFabricatorName(val);
		}
		if (FIELD_NAME_OPERATING_SYSTEM.equals(key)) {
			return getOperatingSystemprovider(val);
		}
		if (FIELD_NAME_IC_TYPE.equals(key)) {
			return getIcTypeName(val);
		}

		if (key.contains("Date")) {
			Date dateVal;
			try {
				dateVal = calculateCplcDate(fromHexString(val));
			} catch (Exception e) {
				return "0x" + val;
			}
			return formatDateOnly(dateVal);
		}
		if (FIELD_NAME_IC_BATCH_ID.equals(key)
				|| FIELD_NAME_OPERATING_SYSTEM_REL_LEVEL.equals(key)) {
			try {
				int decimal = Integer.parseInt(val, 16);
				return Integer.toString(decimal);
			} catch (NumberFormatException nfe) {
				return "0x" + val;
			}
		}
		return "0x" + val;
	}

	/**
	 * @return the parsed fields
	 */
	public Map<String, String> getFields() {
		return fields;
	}

}
