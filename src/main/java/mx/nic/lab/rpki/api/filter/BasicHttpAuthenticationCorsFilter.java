package mx.nic.lab.rpki.api.filter;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;

/**
 * Custom filter used to avoid CORS at protected resources
 *
 */
public class BasicHttpAuthenticationCorsFilter extends BasicHttpAuthenticationFilter {

	@Override
	protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
		if (request instanceof HttpServletRequest) {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			if (httpRequest.getMethod().equalsIgnoreCase("OPTIONS")) {
				return true;
			}
		}
		return super.isAccessAllowed(request, response, mappedValue);
	}
}
