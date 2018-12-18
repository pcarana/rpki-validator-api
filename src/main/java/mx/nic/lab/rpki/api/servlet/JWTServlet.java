package mx.nic.lab.rpki.api.servlet;

import java.io.IOException;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import mx.nic.lab.rpki.api.config.ApiConfiguration;

/**
 * Servlet that acts as a JWT provider, the response is a JSON object with the
 * obtained token: <br>
 * <code>
 * { "token": &lt;JWT as String&gt; }
 * </code>
 *
 */
@WebServlet(name = "jwtprovider", urlPatterns = { "/tokens" })
public class JWTServlet extends HttpServlet {

	/**
	 * Signing key for the JWT created
	 */
	private static Key signingKey;

	/**
	 * Subject - JWT relation, only a subject can have a valid token
	 */
	private static final Map<String, String> tokensSignatures = new HashMap<String, String>();

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	public JWTServlet() {
		super();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String subject = request.getUserPrincipal().getName();
		JwtBuilder builder = Jwts.builder();
		builder.setSubject(subject);
		builder.setIssuer(ApiConfiguration.getJwtIssuer());
		builder.setExpiration(new Date(System.currentTimeMillis() + ApiConfiguration.getJwtExpirationTime()));
		builder.signWith(signingKey, ApiConfiguration.getJwtSignatureAlgorithm());

		String token = builder.compact();
		tokensSignatures.put(subject, token.split("\\.")[2]);
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/json");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().write("{ \"token\": \"" + token + "\" }");
	}

	/**
	 * Set a new signing key using the received {@link SignatureAlgorithm}. The key
	 * is created using {@link Keys#secretKeyFor(SignatureAlgorithm)}
	 * 
	 * @param signatureAlgorithm
	 */
	public static void setSigningKey(SignatureAlgorithm signatureAlgorithm) {
		signingKey = Keys.secretKeyFor(signatureAlgorithm);
	}

	public static Key getSigningKey() {
		return signingKey;
	}

	public static Map<String, String> getTokensSignatures() {
		return tokensSignatures;
	}
}
