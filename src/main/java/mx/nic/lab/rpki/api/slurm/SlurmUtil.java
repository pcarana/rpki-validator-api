package mx.nic.lab.rpki.api.slurm;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;

import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.Hex;

import mx.nic.lab.rpki.api.util.CMSUtil;
import mx.nic.lab.rpki.api.util.Util;
import mx.nic.lab.rpki.db.pojo.ApiObject;
import mx.nic.lab.rpki.db.pojo.SlurmBgpsec;
import mx.nic.lab.rpki.db.pojo.SlurmPrefix;

/**
 * SLURM utilities
 *
 */
public class SlurmUtil {

	private SlurmUtil() {
		// No code
	}

	/**
	 * Validates a JSON object as a SLURM prefix according to the <code>type</code>
	 * specified
	 * 
	 * @param object
	 *            {@link JsonObject} to validate
	 * @param type
	 *            prefix type (filter or assertion)
	 * @return A {@link SlurmPrefix} instance with all its values loaded according
	 *         to the type
	 * @throws IllegalArgumentException
	 *             if there's a validation error
	 */
	public static SlurmPrefix getAndvalidatePrefix(JsonObject object, String type) throws IllegalArgumentException {
		SlurmPrefix slurmPrefix = new SlurmPrefix();
		slurmPrefix.setType(type);
		// Check for extra keys (invalid keys)
		List<String> invalidKeys = new ArrayList<>();
		for (String key : object.keySet()) {
			if (!key.matches("(prefix|asn|maxPrefixLength|comment)")) {
				invalidKeys.add(key);
			} else if (type.equals(SlurmPrefix.TYPE_FILTER) && key.equals("maxPrefixLength")) {
				// Can't be present in a filter
				invalidKeys.add(key);
			}
		}
		if (!invalidKeys.isEmpty()) {
			throw new IllegalArgumentException(
					Util.concatenateParamsToLabel("#{error.invalid.keys}", invalidKeys.toString()));
		}
		String prefixRcv = null;
		try {
			prefixRcv = object.getString("prefix");
		} catch (NullPointerException npe) {
			if (type.equals(SlurmPrefix.TYPE_ASSERTION)) {
				throw new IllegalArgumentException("#{error.slurm.prefix.prefixRequired}");
			}
		} catch (ClassCastException cce) {
			throw new IllegalArgumentException(
					Util.concatenateParamsToLabel("#{error.invalid.dataType}", "prefix", "String"));
		}
		if (prefixRcv != null) {
			String[] prefixArr = prefixRcv.split("/");
			if (prefixArr.length != 2) {
				throw new IllegalArgumentException(
						Util.concatenateParamsToLabel("#{error.invalid.format}", "prefix", "[prefix]/[prefix_length]"));
			}

			try {
				InetAddress prefixAddress = InetAddress.getByName(prefixArr[0]);
				slurmPrefix.setStartPrefix(prefixAddress.getAddress());
				slurmPrefix.setPrefixText(prefixAddress.getHostAddress());
			} catch (UnknownHostException e) {
				throw new IllegalArgumentException("#{error.slurm.prefix.invalid}");
			}
			try {
				int prefixLength = Integer.valueOf(prefixArr[1]);
				slurmPrefix.setPrefixLength(prefixLength);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						Util.concatenateParamsToLabel("#{error.invalid.dataType}", "prefix length", "Number"));
			}
		}
		try {
			// There's no "getLong" method
			JsonNumber number = object.getJsonNumber("asn");
			if (number != null) {
				slurmPrefix.setAsn(number.longValueExact());
			} else if (type.equals(SlurmPrefix.TYPE_ASSERTION)) {
				throw new IllegalArgumentException("#{error.slurm.asnRequired}");
			} else if (slurmPrefix.getStartPrefix() == null) {
				// In a Filter is optional, but either a prefix or an asn must be present
				throw new IllegalArgumentException("#{error.slurm.prefix.prefixOrAsnRequired}");
			}
		} catch (ClassCastException cce) {
			throw new IllegalArgumentException(
					Util.concatenateParamsToLabel("#{error.invalid.dataType}", "asn", "Number"));
		} catch (ArithmeticException e) {
			throw new IllegalArgumentException(Util.concatenateParamsToLabel("#{error.slurm.asnFormat}",
					ApiObject.ASN_MIN_VALUE, ApiObject.ASN_MAX_VALUE));
		}

		try {
			slurmPrefix.setPrefixMaxLength(object.getInt("maxPrefixLength"));
		} catch (NullPointerException npe) {
			// Optional in both cases, do nothing
		} catch (ClassCastException cce) {
			throw new IllegalArgumentException(
					Util.concatenateParamsToLabel("#{error.invalid.dataType}", "maxPrefixLength", "Number"));
		}

		try {
			String value = object.getString("comment");
			if (value.trim().isEmpty()) {
				throw new IllegalArgumentException("#{error.slurm.commentEmpty}");
			}
			slurmPrefix.setComment(value.trim());
		} catch (NullPointerException npe) {
			// It's RECOMMENDED, so (for now) leave it as optional
		} catch (ClassCastException cce) {
			throw new IllegalArgumentException(
					Util.concatenateParamsToLabel("#{error.invalid.dataType}", "comment", "String"));
		}
		return slurmPrefix;
	}

	/**
	 * Validates a JSON object as a SLURM BGPsec according to the <code>type</code>
	 * specified
	 * 
	 * @param object
	 *            {@link JsonObject} to validate
	 * @param type
	 *            prefix type (filter or assertion)
	 * @return A {@link SlurmBgpsec} instance with all its values loaded according
	 *         to the type
	 * @throws IllegalArgumentException
	 *             if there's a validation error
	 */
	public static SlurmBgpsec getAndvalidateBgpsec(JsonObject object, String type) throws IllegalArgumentException {
		SlurmBgpsec slurmBgpsec = new SlurmBgpsec();
		slurmBgpsec.setType(type);
		// Check for extra keys (invalid keys)
		List<String> invalidKeys = new ArrayList<>();
		for (String key : object.keySet()) {
			if (!key.matches("(asn|SKI|routerPublicKey|comment)")) {
				invalidKeys.add(key);
			} else if (type.equals(SlurmBgpsec.TYPE_FILTER) && key.equals("routerPublicKey")) {
				// Can't be present in a filter
				invalidKeys.add(key);
			}
		}
		if (!invalidKeys.isEmpty()) {
			throw new IllegalArgumentException(
					Util.concatenateParamsToLabel("#{error.invalid.keys}", invalidKeys.toString()));
		}

		try {
			// There's no "getLong" method
			JsonNumber number = object.getJsonNumber("asn");
			if (number != null) {
				slurmBgpsec.setAsn(number.longValueExact());
			} else if (type.equals(SlurmBgpsec.TYPE_ASSERTION)) {
				throw new IllegalArgumentException("#{error.slurm.asnRequired}");
			}
		} catch (ClassCastException cce) {
			throw new IllegalArgumentException(
					Util.concatenateParamsToLabel("#{error.invalid.dataType}", "asn", "Number"));
		} catch (ArithmeticException e) {
			throw new IllegalArgumentException(Util.concatenateParamsToLabel("#{error.slurm.asnFormat}",
					ApiObject.ASN_MIN_VALUE, ApiObject.ASN_MAX_VALUE));
		}

		try {
			String value = object.getString("SKI");
			// If the value is sent, it can't be an empty value
			if (value.trim().isEmpty()) {
				throw new IllegalArgumentException("#{error.slurm.bgpsec.skiEmpty}");
			}
			slurmBgpsec.setSki(value.trim());
		} catch (NullPointerException npe) {
			if (type.equals(SlurmBgpsec.TYPE_ASSERTION)) {
				throw new IllegalArgumentException("#{error.slurm.bgpsec.skiRequired}");
			} else if (slurmBgpsec.getAsn() == null) {
				// In a Filter is optional, but either an asn or a SKI must be present
				throw new IllegalArgumentException("#{error.slurm.bgpsec.asnOrSkiRequired}");
			}
		} catch (ClassCastException cce) {
			throw new IllegalArgumentException(
					Util.concatenateParamsToLabel("#{error.invalid.dataType}", "SKI", "String"));
		}

		try {
			String value = object.getString("routerPublicKey");
			if (value.trim().isEmpty()) {
				throw new IllegalArgumentException("#{error.slurm.bgpsec.routerPublicKeyEmpty}");
			}
			slurmBgpsec.setRouterPublicKey(value.trim());
		} catch (NullPointerException npe) {
			if (type.equals(SlurmBgpsec.TYPE_ASSERTION)) {
				throw new IllegalArgumentException("#{error.slurm.bgpsec.routerPublicKeyRequired}");
			}
		} catch (ClassCastException cce) {
			throw new IllegalArgumentException(
					Util.concatenateParamsToLabel("#{error.invalid.dataType}", "routerPublicKey", "String"));
		}

		try {
			String value = object.getString("comment");
			if (value.trim().isEmpty()) {
				throw new IllegalArgumentException("#{error.slurm.commentEmpty}");
			}
			slurmBgpsec.setComment(value.trim());
		} catch (NullPointerException npe) {
			// It's RECOMMENDED, so (for now) leave it as optional
		} catch (ClassCastException cce) {
			throw new IllegalArgumentException(
					Util.concatenateParamsToLabel("#{error.invalid.dataType}", "comment", "String"));
		}

		// Check SKI and routerPublicKey are sent base64 encoded, and verify its
		// value
		if (slurmBgpsec.getSki() != null && !slurmBgpsec.getSki().trim().isEmpty()) {
			try {
				byte[] decodedSki = Base64.getDecoder().decode(slurmBgpsec.getSki().getBytes());
				byte[] hexBytes = Hex.decode(decodedSki);
				// Is the 160-bit SHA-1 hash (RFC 8416 section 3.3.2 citing RFC 6487 section
				// 4.8.2)
				if (hexBytes.length != 20) {
					throw new IllegalArgumentException("#{error.slurm.bgpsec.skiInvalid}");
				}
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(
						Util.concatenateParamsToLabel("#{error.slurm.bgpsec.notBase64}", "SKI"));
			} catch (DecoderException e) {
				throw new IllegalArgumentException(
						Util.concatenateParamsToLabel("#{error.slurm.bgpsec.notHex}", "SKI"));
			}
		}

		if (slurmBgpsec.getRouterPublicKey() != null && !slurmBgpsec.getRouterPublicKey().trim().isEmpty()) {
			try {
				byte[] decodedPk = Base64.getDecoder().decode(slurmBgpsec.getRouterPublicKey().getBytes());
				if (!CMSUtil.isValidSubjectPublicKey(decodedPk)) {
					throw new IllegalArgumentException("#{error.slurm.bgpsec.routerPublicKeyInvalid}");
				}
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(
						Util.concatenateParamsToLabel("#{error.slurm.bgpsec.notBase64}", "routerPublicKey"));
			}
		}
		return slurmBgpsec;
	}

	/**
	 * Validate the JSON object as a SLURM file
	 * 
	 * @param jsonObject
	 *            JSON object representing a SLURM
	 * @param exceptions
	 *            {@link List} of exceptions where any {@link Exception} found will
	 *            be concatenated
	 * @return <code>boolean</code> to indicate the validity of the object
	 */
	public static boolean isValidSlurm(JsonObject jsonObject, List<Exception> exceptions) {
		if (jsonObject.keySet().size() != 3) {
			exceptions.add(new IllegalArgumentException("The JSON object must contain exactly 3 properties: "
					+ "slurmVersion, validationOutputFilters, and locallyAddedAssertions"));
			return false;
		}
		for (String key : jsonObject.keySet()) {
			if (!key.matches("(slurmVersion|validationOutputFilters|locallyAddedAssertions)")) {
				exceptions.add(new IllegalArgumentException("Invalid key '" + key + "' at JSON object"));
				return false;
			}
			switch (key) {
			case "slurmVersion":
				try {
					if (jsonObject.getInt(key) != 1) {
						exceptions.add(new IllegalArgumentException("'" + key + "' must have the value '1'"));
						return false;
					}
				} catch (ClassCastException e) {
					exceptions.add(new IllegalArgumentException("'" + key + "' must a number with value '1'"));
					return false;
				}
				break;
			case "validationOutputFilters":
				JsonObject filters = null;
				try {
					filters = jsonObject.getJsonObject(key);
				} catch (ClassCastException e) {
					exceptions.add(new IllegalArgumentException("'" + key + "' must be a JSON object"));
					return false;
				}
				if (!areValidFilters(filters, exceptions)) {
					return false;
				}
				break;
			case "locallyAddedAssertions":
				JsonObject assertions = null;
				try {
					assertions = jsonObject.getJsonObject(key);
				} catch (ClassCastException e) {
					exceptions.add(new IllegalArgumentException("'" + key + "' must be a JSON object"));
					return false;
				}
				if (!areValidAssertions(assertions, exceptions)) {
					return false;
				}
				break;
			}
		}
		return true;
	}

	/**
	 * Validate the JSON object as the property 'validationOutputFilters' of the
	 * SLURM
	 * 
	 * @param jsonObject
	 *            JSON object representing 'validationOutputFilters'
	 * @param exceptions
	 *            {@link List} of exceptions where any {@link Exception} found will
	 *            be concatenated
	 * @return <code>boolean</code> to indicate the validity of the object
	 */
	private static boolean areValidFilters(JsonObject jsonObject, List<Exception> exceptions) {
		if (jsonObject.keySet().size() != 2) {
			exceptions.add(new IllegalArgumentException(
					"The JSON object 'validationOutputFilters' must contain exactly 2 properties: "
							+ "prefixFilters, and bgpsecFilters"));
			return false;
		}
		for (String key : jsonObject.keySet()) {
			if (!key.matches("(prefixFilters|bgpsecFilters)")) {
				exceptions.add(new IllegalArgumentException(
						"Invalid key '" + key + "' at JSON object 'validationOutputFilters'"));
				return false;
			}
			switch (key) {
			case "prefixFilters":
				JsonArray prefixes = null;
				try {
					prefixes = jsonObject.getJsonArray(key);
				} catch (ClassCastException e) {
					exceptions.add(new IllegalArgumentException("'" + key + "' must be a JSON array"));
					return false;
				}
				if (!areValidPrefixes(prefixes, SlurmPrefix.TYPE_FILTER, exceptions)) {
					return false;
				}
				break;
			case "bgpsecFilters":
				JsonArray bgpsecs = null;
				try {
					bgpsecs = jsonObject.getJsonArray(key);
				} catch (ClassCastException e) {
					exceptions.add(new IllegalArgumentException("'" + key + "' must be a JSON array"));
					return false;
				}
				if (!areValidBgpsecs(bgpsecs, SlurmBgpsec.TYPE_FILTER, exceptions)) {
					return false;
				}
				break;
			}
		}
		return true;
	}

	/**
	 * Validate the JSON object as the property 'locallyAddedAssertions' of the
	 * SLURM
	 * 
	 * @param jsonObject
	 *            JSON object representing 'locallyAddedAssertions'
	 * @param exceptions
	 *            {@link List} of exceptions where any {@link Exception} found will
	 *            be concatenated
	 * @return <code>boolean</code> to indicate the validity of the object
	 */
	private static boolean areValidAssertions(JsonObject jsonObject, List<Exception> exceptions) {
		if (jsonObject.keySet().size() != 2) {
			exceptions.add(new IllegalArgumentException(
					"The JSON object 'locallyAddedAssertions' must contain exactly 2 properties: "
							+ "prefixAssertions, and bgpsecAssertions"));
			return false;
		}
		for (String key : jsonObject.keySet()) {
			if (!key.matches("(prefixAssertions|bgpsecAssertions)")) {
				exceptions.add(new IllegalArgumentException(
						"Invalid key '" + key + "' at JSON object 'locallyAddedAssertions'"));
				return false;
			}
			switch (key) {
			case "prefixAssertions":
				JsonArray prefixes = null;
				try {
					prefixes = jsonObject.getJsonArray(key);
				} catch (ClassCastException e) {
					exceptions.add(new IllegalArgumentException("'" + key + "' must be a JSON array"));
					return false;
				}
				if (!areValidPrefixes(prefixes, SlurmPrefix.TYPE_ASSERTION, exceptions)) {
					return false;
				}
				break;
			case "bgpsecAssertions":
				JsonArray bgpsecs = null;
				try {
					bgpsecs = jsonObject.getJsonArray(key);
				} catch (ClassCastException e) {
					exceptions.add(new IllegalArgumentException("'" + key + "' must be a JSON array"));
					return false;
				}
				if (!areValidBgpsecs(bgpsecs, SlurmBgpsec.TYPE_ASSERTION, exceptions)) {
					return false;
				}
				break;
			}
		}
		return true;
	}

	/**
	 * Validate all the prefixes contained at the SLURM
	 * 
	 * @param jsonArray
	 *            array of prefixes
	 * @param type
	 *            type of prefixes to validate (filter or assertion)
	 * @param exceptions
	 * @return <code>boolean</code> to indicate if all the objects were valid
	 */
	private static boolean areValidPrefixes(JsonArray jsonArray, String type, List<Exception> exceptions) {
		boolean allValid = true;
		JsonObject object = null;
		for (int i = 0; i < jsonArray.size(); i++) {
			try {
				object = jsonArray.getJsonObject(i);
				getAndvalidatePrefix(object, type);
			} catch (ClassCastException e) {
				exceptions.add(
						new IllegalArgumentException("The prefix " + type + " #" + (i + 1) + " must be a JSON object"));
				allValid = false;
			} catch (IllegalArgumentException e) {
				String message = Util.getJsonWithLocale(Locale.getDefault(), "\"" + e.getMessage() + "\"");
				exceptions.add(new IllegalArgumentException(
						"The prefix " + type + " #" + (i + 1) + " has the error: " + message));
				allValid = false;
			}
		}
		return allValid;
	}

	/**
	 * Validate all the BGPsec's contained at the SLURM
	 * 
	 * @param jsonArray
	 *            array of BGPsec's
	 * @param type
	 *            type of BGPsec's to validate (filter or assertion)
	 * @param exceptions
	 * @return <code>boolean</code> to indicate if all the objects were valid
	 */
	private static boolean areValidBgpsecs(JsonArray jsonArray, String type, List<Exception> exceptions) {
		boolean allValid = true;
		JsonObject object = null;
		for (int i = 0; i < jsonArray.size(); i++) {
			try {
				object = jsonArray.getJsonObject(i);
				getAndvalidateBgpsec(object, type);
			} catch (ClassCastException e) {
				exceptions.add(
						new IllegalArgumentException("The bgpsec " + type + " #" + (i + 1) + " must be a JSON object"));
				allValid = false;
			} catch (IllegalArgumentException e) {
				String message = Util.getJsonWithLocale(Locale.getDefault(), "\"" + e.getMessage() + "\"");
				exceptions.add(new IllegalArgumentException(
						"The bgpsec " + type + " #" + (i + 1) + " has the error: " + message));
				allValid = false;
			}
		}
		return allValid;
	}

}
