package mx.nic.lab.rpki.api.result;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;
import javax.json.JsonValue;

/**
 * Generic result from the API
 *
 */
public abstract class ApiResult {

	/**
	 * Return the JSON of the result
	 * 
	 * @return {@link JsonStructure} of the object
	 */
	public abstract JsonStructure toJsonStructure();

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
	protected void addKeyValueToBuilder(JsonObjectBuilder builder, String key, Object value, boolean addNullValue) {
		if (value != null) {
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
}
