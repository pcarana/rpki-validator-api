package mx.nic.lab.rpki.api.servlet;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.exception.BadRequestException;
import mx.nic.lab.rpki.api.util.Util;
import mx.nic.lab.rpki.db.pojo.ApiObject;

/**
 * Represents the paging and ordering parameters received from a request
 *
 */
public class PagingParameters {

	/**
	 * Query parameter name to indicate limit
	 */
	public static final String LIMIT = "limit";

	/**
	 * Query parameter name to indicate offset
	 */
	public static final String OFFSET = "offset";

	/**
	 * Query parameter name to indicate the properties used to sort as well as the
	 * order (asc, desc)
	 */
	public static final String SORT = "sort";

	/**
	 * Desired result limit
	 */
	private int limit;

	/**
	 * Desired result offset (commonly used in conjunction with limit)
	 */
	private int offset;

	/**
	 * Columns used for sort, the order is important
	 */
	private LinkedHashMap<String, String> sort;

	/**
	 * Can only be instantiated from here
	 */
	private PagingParameters() {
		this.limit = -1;
		this.offset = -1;
		this.sort = null;
	}

	/**
	 * Get an instance from the received {@link HttpServletRequest}, only 3 query
	 * parameters are expected: limit, offset and sort; where:<br>
	 * <ul>
	 * <li>limit: must be a positive integer (excluding 0)
	 * <li>offset: must be a positive integer (including 0) and can't be present if
	 * limit isn't valid nor existent
	 * <li>sort: comma separated values of the properties used for sorting, a
	 * preceding sign can be used to indicate the ordering term ('+' means 'asc',
	 * '-' means 'desc'), if no sign is used then the default value is 'asc'. The
	 * sent order of the parameters is honored. E.g. "?sort=+id,-date,name" means
	 * that the ordering will be 'id asc, date desc, name asc'. The
	 * <code>validSortKeysMap</code> is used to validate that the properties sent
	 * are expected by the corresponding servlet.
	 * </ul>
	 * None of the query parameters is mandatory, but if they're sent then they must
	 * be valid. An empty value is treated as invalid.
	 * 
	 * @param request
	 *            Request from the client
	 * @param validSortKeysMap
	 *            Used to validate the properties that can be used to sort, the key
	 *            is the name of the expected parameter and the value is the
	 *            corresponding POJO property that the DA implementation can use to
	 *            effectively sort
	 * @return a new {@link PagingParameters} instance
	 * @throws BadRequestException
	 *             if the query parameters aren't as expected (empty parameters
	 *             aren't valid)
	 */
	public static PagingParameters createFromRequest(HttpServletRequest request, Map<String, String> validSortKeysMap)
			throws BadRequestException {
		// If sent multiple times, than only take into account the first parameter
		String rcvdLimit = request.getParameter(LIMIT);
		String rcvdOffset = request.getParameter(OFFSET);
		String rcvdSort = request.getParameter(SORT);

		PagingParameters pagingParameters = new PagingParameters();
		if (rcvdLimit != null) {
			try {
				pagingParameters.setLimit(Integer.parseInt(rcvdLimit));
			} catch (NumberFormatException e) {
				throw new BadRequestException(
						Util.concatenateParamsToLabel("#{error.invalid.dataType}", LIMIT, "integer"));
			}
			// Only values greater than 0 are accepted
			if (pagingParameters.getLimit() < 1) {
				throw new BadRequestException(Util.concatenateParamsToLabel("#{error.paging.minValue}", LIMIT, "1"));
			}
		}
		// limit can be received without offset
		if (rcvdOffset != null) {
			try {
				pagingParameters.setOffset(Integer.parseInt(rcvdOffset));
			} catch (NumberFormatException e) {
				throw new BadRequestException(
						Util.concatenateParamsToLabel("#{error.invalid.dataType}", OFFSET, "integer"));
			}
			// ...but offset can't be received without limit
			if (pagingParameters.getLimit() == -1) {
				throw new BadRequestException("#{error.paging.offsetWithoutLimit}");
			}
			// Only positive values are accepted (including 0)
			if (pagingParameters.getOffset() < 0) {
				throw new BadRequestException(Util.concatenateParamsToLabel("#{error.paging.minValue}", OFFSET, "0"));
			}
		}
		if (rcvdSort != null) {
			if (rcvdSort.trim().isEmpty()) {
				throw new BadRequestException("#{error.paging.emptySort}");
			}
			String[] rcvdCols = rcvdSort.trim().split(",");
			LinkedHashMap<String, String> paramMap = new LinkedHashMap<>();
			for (String col : rcvdCols) {
				col = col.trim();
				if (col.isEmpty()) {
					throw new BadRequestException("#{error.paging.emptySort}");
				}
				char sign = col.charAt(0);
				String ordering = sign == '-' ? ApiObject.ORDER_DESC : ApiObject.ORDER_ASC;
				String key = sign == '-' || sign == '+' ? col.substring(1) : col;
				// Validate the sort keys with the expected
				if (!validSortKeysMap.containsKey(key)) {
					throw new BadRequestException(
							Util.concatenateParamsToLabel("#{error.paging.invalidColumnSort}", key));
				}
				// Check for duplicates
				if (paramMap.containsKey(key)) {
					throw new BadRequestException(Util.concatenateParamsToLabel("#{error.paging.duplicatedSort}", key));
				}
				paramMap.put(validSortKeysMap.get(key), ordering);
			}
			if (!paramMap.isEmpty()) {
				pagingParameters.setSort(paramMap);
			}
		}
		return pagingParameters;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public LinkedHashMap<String, String> getSort() {
		return sort;
	}

	public void setSort(LinkedHashMap<String, String> sort) {
		this.sort = sort;
	}
}
