package mx.nic.lab.rpki.api.servlet.tree;

import java.util.Arrays;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import mx.nic.lab.rpki.api.exception.BadRequestException;
import mx.nic.lab.rpki.api.exception.HttpException;
import mx.nic.lab.rpki.api.result.ApiResult;
import mx.nic.lab.rpki.api.result.tree.CertificateTreeResult;
import mx.nic.lab.rpki.api.servlet.DataAccessServlet;
import mx.nic.lab.rpki.api.servlet.RequestMethod;
import mx.nic.lab.rpki.api.util.Util;
import mx.nic.lab.rpki.db.cert.tree.CertificationTreeNode;
import mx.nic.lab.rpki.db.exception.ApiDataAccessException;
import mx.nic.lab.rpki.db.pojo.PagingParameters;
import mx.nic.lab.rpki.db.service.DataAccessService;
import mx.nic.lab.rpki.db.spi.CertificateTreeDAO;

/**
 * Servlet to provide (all using the GET method):<br>
 * <li>The direct childs of a TAL certificate, using the URI
 * <code>/tree/root/&lt;id&gt;</code>
 * <li>The childs of a child certificate (a certificate that's distinct than the
 * loaded TAL certificate), using the URI <code>/tree/child/&lt;id&gt;</code>
 *
 */
@WebServlet(name = "certificateTree", urlPatterns = { "/tree/*" })
public class CertificateTreeServlet extends DataAccessServlet<CertificateTreeDAO> {

	/**
	 * Constant to define the service URI to get the certification tree using the
	 * TALs loaded certificate as root
	 */
	protected static final String ROOT_SERVICE = "root";

	/**
	 * Constant to define the service URI to get the certification tree using a
	 * certificate (distinct than the TALs loaded certificate) as root
	 */
	protected static final String CHILD_SERVICE = "child";

	/**
	 * Serial version ID
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected ApiResult doApiDaRequest(RequestMethod requestMethod, HttpServletRequest request, CertificateTreeDAO dao)
			throws HttpException, ApiDataAccessException {
		List<String> additionalPathInfo = Util.getAdditionaPathInfo(request, 2, false);
		if (additionalPathInfo.size() != 2) {
			throw new BadRequestException("#{error.missingArguments}");
		}
		String type = additionalPathInfo.get(0);
		if (!(type.equals(ROOT_SERVICE) || type.equals(CHILD_SERVICE))) {
			// Invalid service
			return null;
		}
		Long id = null;
		try {
			id = Long.parseLong(additionalPathInfo.get(1));
		} catch (NumberFormatException e) {
			throw new BadRequestException("#{error.invalidId}", e);
		}
		// Only the page parameters are expected (limit and offset)
		PagingParameters pagingParameters = getPagingParameters(request);
		CertificationTreeNode tree = null;
		if (type.equals(ROOT_SERVICE)) {
			tree = dao.getFromRoot(id, pagingParameters);
		} else {
			tree = dao.getFromChild(id, pagingParameters);
		}
		if (tree == null) {
			return null;
		}
		return new CertificateTreeResult(tree, pagingParameters);
	}

	@Override
	protected String getServedObjectName() {
		return "certificateTree";
	}

	@Override
	protected List<RequestMethod> getSupportedRequestMethods() {
		return Arrays.asList(RequestMethod.GET);
	}

	@Override
	protected CertificateTreeDAO initAccessDAO() throws ApiDataAccessException {
		return DataAccessService.getCertificateTreeDAO();
	}

}
