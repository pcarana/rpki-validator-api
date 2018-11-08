package mx.nic.lab.rpki.api.result.tree;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import org.bouncycastle.util.encoders.Hex;

import mx.nic.lab.rpki.api.result.ApiSingleResult;
import mx.nic.lab.rpki.db.cert.tree.CertificateNode;
import mx.nic.lab.rpki.db.cert.tree.CertificationTreeNode;
import mx.nic.lab.rpki.db.cert.tree.GbrNode;
import mx.nic.lab.rpki.db.cert.tree.ResourceNode;
import mx.nic.lab.rpki.db.cert.tree.RoaNode;

/**
 * Result that represents a certification tree, either as the TAL as root or as
 * another certificate as root
 *
 */
public class CertificateTreeResult extends ApiSingleResult<CertificationTreeNode> {

	public CertificateTreeResult(CertificationTreeNode certificationTreeNode) {
		super();
		setApiObject(certificationTreeNode);
	}

	@Override
	public JsonStructure toJsonStructure() {
		// The root object is necessarily a CertificateNode
		CertificationTreeNode certificationTreeNode = getApiObject();
		if (certificationTreeNode == null || !(certificationTreeNode instanceof CertificateNode)) {
			return null;
		}
		CertificateNode certificateNode = (CertificateNode) certificationTreeNode;
		JsonObjectBuilder rootBuilder = Json.createObjectBuilder();
		addCommonFields(rootBuilder, certificateNode);
		addCertificateWithoutChilds(rootBuilder, certificateNode);
		if (certificateNode.getChildCount() > 0) {
			JsonArrayBuilder childsBuilder = Json.createArrayBuilder();
			for (CertificationTreeNode child : certificateNode.getChilds()) {
				childsBuilder.add(buildChild(child));
			}
			addKeyValueToBuilder(rootBuilder, "childs", childsBuilder, false);
		}
		return rootBuilder.build();
	}

	/**
	 * Build the JSON representation of a child node, the representation is made
	 * according to the node type (ROA, CER, GBR, ...)
	 * 
	 * @param child
	 * @return
	 */
	private JsonStructure buildChild(CertificationTreeNode child) {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		addCommonFields(builder, child);
		if (child instanceof CertificateNode) {
			addCertificateWithoutChilds(builder, (CertificateNode) child);
		} else if (child instanceof RoaNode) {
			RoaNode roaNode = (RoaNode) child;
			JsonArrayBuilder resources = Json.createArrayBuilder();
			for (ResourceNode resourceNode : roaNode.getResources()) {
				JsonObjectBuilder resource = Json.createObjectBuilder();
				addKeyValueToBuilder(resource, "asn", resourceNode.getAsn(), false);
				addKeyValueToBuilder(resource, "prefix", resourceNode.getPrefix(), false);
				addKeyValueToBuilder(resource, "maxPrefixLength", resourceNode.getMaxPrefixLength(), false);
				addKeyValueToBuilder(resource, "roaId", resourceNode.getRoaId(), false);
				resources.add(resource);
			}
			addKeyValueToBuilder(builder, "resources", resources, false);
		} else if (child instanceof GbrNode) {
			GbrNode gbrNode = (GbrNode) child;
			addKeyValueToBuilder(builder, "vcard", gbrNode.getVCard(), false);
		}
		return builder.build();
	}

	/**
	 * Add the JSON representation of a certificate node to the corresponding
	 * builder
	 * 
	 * @param builder
	 * @param certificate
	 */
	private void addCertificateWithoutChilds(JsonObjectBuilder builder, CertificateNode certificate) {
		addKeyValueToBuilder(builder, "id", certificate.getId(), true);
		addKeyValueToBuilder(builder, "childCount", certificate.getChildCount(), true);
	}

	/**
	 * Add the common fields of all the nodes
	 * 
	 * @param builder
	 * @param node
	 */
	private void addCommonFields(JsonObjectBuilder builder, CertificationTreeNode node) {
		addKeyValueToBuilder(builder, "type", node.getType(), true);
		JsonArrayBuilder locationsBuilder = Json.createArrayBuilder();
		for (String location : node.getLocations()) {
			locationsBuilder.add(location);
		}
		addKeyValueToBuilder(builder, "locations", locationsBuilder, true);
		if (node.getSubjectKeyIdentifier() != null) {
			addKeyValueToBuilder(builder, "subjectKeyIdentifier",
					Hex.toHexString(node.getSubjectKeyIdentifier()).toUpperCase(), false);
		}
	}
}
