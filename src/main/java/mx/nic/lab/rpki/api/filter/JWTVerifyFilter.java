package mx.nic.lab.rpki.api.filter;

import java.util.Date;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.web.filter.AccessControlFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import mx.nic.lab.rpki.api.config.ApiConfiguration;
import mx.nic.lab.rpki.api.servlet.JWTServlet;

/**
 * {@link AccessControlFilter} to verify a JWT that's received at the
 * "Authorization" header
 *
 */
public class JWTVerifyFilter extends AccessControlFilter {

	@Override
	protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue)
			throws Exception {
		// Exclusive for CORS
		if (request instanceof HttpServletRequest) {
			HttpServletRequest httpRequest = (HttpServletRequest) request;
			if (httpRequest.getMethod().equalsIgnoreCase("OPTIONS")) {
				return true;
			}
		}
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		String jwt = httpRequest.getHeader("Authorization");
		if (jwt == null || !jwt.startsWith("Bearer ")) {
			return false;
		}
		jwt = jwt.substring(jwt.indexOf(" "));
		Jws<Claims> jwsClaims = null;
		try {
			jwsClaims = Jwts.parser().setSigningKey(JWTServlet.getSigningKey()).parseClaimsJws(jwt);
		} catch (UnsupportedJwtException e) {
			// if the claimsJws argument does not represent an Claims JWS
			return false;
		} catch (MalformedJwtException e) {
			// if the claimsJws string is not a valid JWS
			return false;
		} catch (SignatureException e) {
			// if the claimsJws JWS signature validation fails
			return false;
		} catch (ExpiredJwtException e) {
			// if the specified JWT is a Claims JWT and the Claims has an expiration time
			// before the time this method is invoked.
			return false;
		} catch (IllegalArgumentException e) {
			// if the claimsJws string is null or empty or only whitespace
			return false;
		}
		Claims claims = jwsClaims.getBody();
		// Double check of the expiration
		if (claims.getExpiration() == null || new Date().after(claims.getExpiration())) {
			return false;
		}
		// Issuer check
		if (claims.getIssuer() == null || !claims.getIssuer().equals(ApiConfiguration.getJwtIssuer())) {
			return false;
		}
		// Must have a subject
		if (claims.getSubject() == null) {
			return false;
		}
		// And the subject has received that token
		String sentSignature = JWTServlet.getTokensSignatures().get(claims.getSubject());
		if (sentSignature == null || !sentSignature.equals(jwsClaims.getSignature())) {
			return false;
		}
		return true;
	}

	@Override
	protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		return false;
	}

}
