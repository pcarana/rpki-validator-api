package mx.nic.lab.rpki.api.result.roa;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.cms.SignerIdentifier;
import org.bouncycastle.asn1.cms.SignerInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.encoders.Hex;

import mx.nic.lab.rpki.api.result.ApiSingleResult;
import mx.nic.lab.rpki.db.pojo.Gbr;
import mx.nic.lab.rpki.db.pojo.Roa;

/**
 * Result that represents a single Roa
 *
 */
public class RoaResult extends ApiSingleResult<Roa> {

	private static final Logger logger = Logger.getLogger(RoaResult.class.getName());

	public RoaResult(Roa roa) {
		super();
		setApiObject(roa);
	}

	@Override
	public JsonStructure toJsonStructure() {
		Roa roa = getApiObject();
		if (roa == null) {
			return JsonObject.EMPTY_JSON_OBJECT;
		}
		JsonObjectBuilder builder = Json.createObjectBuilder();
		if (roa.getId() != null) {
			builder.add("id", roa.getId());
		} else {
			builder.addNull("id");
		}
		if (roa.getAsn() != null) {
			builder.add("asn", roa.getAsn());
		} else {
			builder.addNull("asn");
		}
		if (roa.getPrefixText() != null) {
			builder.add("prefix", roa.getPrefixText());
		} else {
			builder.addNull("prefix");
		}
		if (roa.getPrefixLength() != null) {
			builder.add("prefixLength", roa.getPrefixLength());
		} else {
			builder.addNull("prefixLength");
		}
		if (roa.getPrefixMaxLength() != null) {
			builder.add("prefixMaxLength", roa.getPrefixMaxLength());
		} else {
			builder.addNull("prefixMaxLength");
		}
		JsonObject cmsObject = getCmsAsJson(roa.getCmsData());
		if (cmsObject != null) {
			builder.add("cms", cmsObject);
		} else {
			builder.addNull("cms");
		}
		buildRoaGbrs(builder, roa);

		return builder.build();
	}

	/**
	 * Adds the {@link Gbr} list of the {@link Roa} as a JSON Array to the
	 * <code>JsonObjectBuilder</code> sent
	 * 
	 * @param builder
	 * @param roa
	 */
	protected void buildRoaGbrs(JsonObjectBuilder builder, Roa roa) {
		if (roa.getGbrs() == null || roa.getGbrs().isEmpty()) {
			builder.add("gbr", JsonObject.EMPTY_JSON_ARRAY);
			return;
		}
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		for (Gbr gbr : roa.getGbrs()) {
			JsonObjectBuilder objBuilder = Json.createObjectBuilder();
			if (gbr.getVcard() != null) {
				builder.add("vcard", gbr.getVcard());
			} else {
				builder.addNull("vcard");
			}
			JsonObject cmsObject = getCmsAsJson(gbr.getCmsData());
			if (cmsObject != null) {
				builder.add("cms", cmsObject);
			} else {
				builder.addNull("cms");
			}
			arrayBuilder.add(objBuilder);
		}
		builder.add("gbr", arrayBuilder);
	}

	private JsonObject getCmsAsJson(byte[] cmsData) {
		if (cmsData == null) {
			return null;
		}
		// First, try to parse the CMS
		ASN1InputStream aIn = new ASN1InputStream(new ByteArrayInputStream(cmsData));
		ContentInfo info = null;
		try {
			try {
				info = ContentInfo.getInstance(aIn.readObject());
			} catch (IOException e) {
				logger.log(Level.WARNING, "The CMS data couldn't be parsed, sending null CMS", e);
				return null;
			}
		} finally {
			if (aIn != null) {
				try {
					aIn.close();
				} catch (IOException e) {
					logger.log(Level.WARNING, "Error closing ASN1InputStream, still sending CMS data in response", e);
				}
			}
		}

		SignedData sData = null;
		JsonObjectBuilder cmsBuilder = Json.createObjectBuilder();
		JsonObjectBuilder contentBuilder = Json.createObjectBuilder();
		JsonArrayBuilder genericArrayBuilder = null;
		JsonObjectBuilder genericObjectBuilder = null;

		cmsBuilder.add("contentType", info.getContentType().getId());
		sData = SignedData.getInstance(info.getContent());
		contentBuilder.add("version", sData.getVersion().getValue());

		// RFC 6488 "This set MUST contain exactly one digest algorithm OID"
		genericArrayBuilder = Json.createArrayBuilder();
		ASN1Sequence asn1Seq = ASN1Sequence.getInstance(sData.getDigestAlgorithms().getObjectAt(0));
		genericArrayBuilder.add(asn1Seq.getObjectAt(0).toString());
		contentBuilder.add("digestAlgorithms", genericArrayBuilder);

		genericObjectBuilder = Json.createObjectBuilder();
		genericObjectBuilder.add("eContentType", sData.getEncapContentInfo().getContentType().getId());
		// FIXME Each object (ROA, MFT, GBR) will define this content
		genericObjectBuilder.add("eContent", sData.getEncapContentInfo().getContent().toString());
		contentBuilder.add("encapContentInfo", genericObjectBuilder);

		// RFC 6488 "MUST contain exactly one certificate, the RPKI end-entity (EE)"
		genericArrayBuilder = Json.createArrayBuilder();
		Certificate cert = Certificate.getInstance(sData.getCertificates().getObjectAt(0));
		genericArrayBuilder.add(getCertificateAsJson(cert));
		contentBuilder.add("certificates", genericArrayBuilder);

		// RFC 6488 "the SignerInfos set MUST contain only a single SignerInfo
		// object"
		genericArrayBuilder = Json.createArrayBuilder();
		genericObjectBuilder = Json.createObjectBuilder();
		SignerInfo signerInfo = SignerInfo.getInstance(sData.getSignerInfos().getObjectAt(0));
		genericObjectBuilder.add("version", signerInfo.getVersion().getValue());

		// RFC 6488 "For RPKI signed objects, the sid MUST be the
		// SubjectKeyIdentifier that appears in the EE certificate carried in the CMS
		// certificates field."
		SignerIdentifier signerIdentifier = SignerIdentifier.getInstance(signerInfo.getSID());
		SubjectKeyIdentifier ski = SubjectKeyIdentifier.getInstance(signerIdentifier.getId());
		genericObjectBuilder.add("sid", Strings.fromByteArray(Hex.encode(ski.getKeyIdentifier())).toUpperCase());

		AlgorithmIdentifier algIdentifier = signerInfo.getDigestAlgorithm();
		JsonObjectBuilder internalTempBuilder = Json.createObjectBuilder();
		internalTempBuilder.add("algorithm", algIdentifier.getAlgorithm().getId());
		if (algIdentifier.getParameters() != null) {
			internalTempBuilder.add("parameters", algIdentifier.getParameters().toString());
		}

		genericObjectBuilder.add("digestAlgorithm", internalTempBuilder);

		// RFC 6488 "The signedAttrs element MUST be present and MUST include the
		// content-type and message-digest attributes [RFC5652]. The signer MAY also
		// include the signing-time attribute [RFC5652], the binary-signing-time
		// attribute [RFC6019], or both attributes. Other signed attributes MUST NOT be
		// included." more at section 2.1.6.4
		Iterator<ASN1Encodable> iterator = signerInfo.getAuthenticatedAttributes().iterator();
		JsonArrayBuilder internalArrBuilder = Json.createArrayBuilder();
		internalTempBuilder = null;
		while (iterator.hasNext()) {
			internalTempBuilder = Json.createObjectBuilder();
			Attribute attr = Attribute.getInstance(iterator.next());
			String attributeId = attr.getAttrType().getId();
			internalTempBuilder.add("attrType", attributeId);
			// RFC 6488 "in an RPKI signed object, the attrValues MUST consist of only a
			// single AttributeValue"
			ASN1Encodable firstAttrValue = attr.getAttrValues().getObjectAt(0);
			// The toString method works well for: "Content-Type Attribute" and
			// "Signing-Time Attribute"
			String attributeValue = firstAttrValue.toString();
			if (attributeId.equals("1.2.840.113549.1.9.4")) {
				// RFC 6488 2.1.6.4.2. Message-Digest Attribute
				// The toString method adds a "#", remove it and convert to upper case
				attributeValue = attributeValue.replace("#", "").toUpperCase();
			} else if (attributeId.equals("1.2.840.113549.1.9.16.2.46")) {
				// RFC 6488 2.1.6.4.4. Binary-Signing-Time Attribute
				// FIXME Apparently the toString method may work for this value
			}
			internalTempBuilder.add("attrValues", Json.createArrayBuilder().add(attributeValue));
			internalArrBuilder.add(internalTempBuilder);
		}
		genericObjectBuilder.add("signedAttrs", internalArrBuilder);

		genericObjectBuilder.add("signatureAlgorithm",
				signerInfo.getDigestEncryptionAlgorithm().getAlgorithm().toString());

		// The toString method adds a "#", remove it and convert to upper case
		genericObjectBuilder.add("signature",
				signerInfo.getEncryptedDigest().toString().replace("#", "").toUpperCase());

		genericArrayBuilder.add(genericObjectBuilder);
		contentBuilder.add("signerInfos", genericArrayBuilder);

		cmsBuilder.add("content", contentBuilder);

		return cmsBuilder.build();
	}

	private JsonObject getCertificateAsJson(Certificate certificate) {
		// FIXME Complete
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add("version", certificate.getVersionNumber());
		builder.add("issuer", certificate.getIssuer().toString());
		return builder.build();
	}
}
