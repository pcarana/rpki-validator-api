package mx.nic.lab.rpki.api.result;

import mx.nic.lab.rpki.api.util.Util;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;

/**
 * Generic JSON result from the API
 */
public abstract class ApiResult extends ApiResultAbstract {

	/**
	 * Return the JSON of the result
	 *
	 * @return {@link JsonStructure} of the object
	 */
	protected abstract JsonStructure toJsonStructure();

	/**
	 * Add an Object value to the {@link JsonObjectBuilder} with the specified key
	 * using the <code>add()</code> methods defined at {@link JsonObjectBuilder}. If
	 * the value is "null" and the flag <code>addNullValue</code> is set to
	 * <code>true</code> then add the key with null value, if the flag is set to
	 * <code>false</code> then the whole object isn't added.<br>
	 * The expected Object values are:
	 * <li>{@link String}
	 * <li>{@link BigInteger}
	 * <li>{@link BigDecimal}
	 * <li>{@link JsonArrayBuilder}
	 * <li>{@link JsonObjectBuilder}
	 * <li>{@link JsonValue}
	 * <li>{@link Integer}
	 * <li>{@link Long}
	 * <li>{@link Double}
	 * <li>{@link Boolean}
	 * 
	 * @param builder
	 *            {@link JsonObjectBuilder} where the key - value will be added
	 * @param key
	 *            {@link String} to indicate the property key
	 * @param value
	 *            {@link Object} to set the value of the key
	 * @param addNullValue
	 *            <code>boolean</code> to indicate if the property key permits null
	 *            values
	 */
	@SuppressWarnings("rawtypes")
	protected void addKeyValueToBuilder(JsonObjectBuilder builder, String key, Object value, boolean addNullValue) {
		if (value != null) {
			if (value instanceof Enum) {
				builder.add(key, ((Enum) value).toString());
				return;
			}
			if (value instanceof String) {
				builder.add(key, (String) value);
				return;
			}
			if (value instanceof BigInteger) {
				builder.add(key, (BigInteger) value);
				return;
			}
			if (value instanceof BigDecimal) {
				builder.add(key, (BigDecimal) value);
				return;
			}
			if (value instanceof JsonArrayBuilder) {
				builder.add(key, (JsonArrayBuilder) value);
				return;
			}
			if (value instanceof JsonObjectBuilder) {
				builder.add(key, (JsonObjectBuilder) value);
				return;
			}
			if (value instanceof JsonValue) {
				builder.add(key, (JsonValue) value);
				return;
			}
			if (value instanceof Integer) {
				builder.add(key, (int) value);
				return;
			}
			if (value instanceof Long) {
				builder.add(key, (long) value);
				return;
			}
			if (value instanceof Double) {
				builder.add(key, (double) value);
				return;
			}
			if (value instanceof Boolean) {
				builder.add(key, (boolean) value);
				return;
			}
		}
		if (addNullValue) {
			builder.addNull(key);
		}
	}


	@Override
	public void printBody(Locale locale, HttpServletResponse resp) throws IOException {
		// Render RESULT
		resp.setStatus(this.getCode());
		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("application/json");
		resp.setHeader("Access-Control-Allow-Origin", "*");
		JsonStructure jsonResponse = this.toJsonStructure();
		if (jsonResponse != null) {
			String body = getLocaleJson(locale, jsonResponse.toString(), resp);
			resp.getWriter().print(body);
		}
	}


	/**
	 * Get the complete JSON string, replacing all the labels "#{label}" with its
	 * corresponding locale value. If there's no bundle available or no property
	 * defined, an empty String is used to replace the corresponding label.<br>
	 * <br>
	 * If the label has parameters concatenated (e.g. #{label}{param1}{param2} see
	 * more at
	 * {@link mx.nic.lab.rpki.api.util.Util#concatenateParamsToLabel(String, Object...)})
	 * then take those values into account to replace any parameters indicated at
	 * the label. The order of the parameter affects the replacement order, so the
	 * parameter <code>{0}</code> will be replaced with the first param value, the
	 * <code>{1}</code> with the second, and so on... <br>
	 * <br>
	 * Finally, the <code>response</code> is modified adding the 'Content-Language'
	 * header to indicate which language was used.
	 *
	 * @param locale
	 * @param jsonString
	 * @param response
	 * @return JSON string with labels replaced
	 */
	private String getLocaleJson(Locale locale, String jsonString, HttpServletResponse response) {
		jsonString = Util.getJsonWithLocale(locale, jsonString);
		response.setHeader("Content-Language", locale.toLanguageTag());
		return jsonString;
	}

}
