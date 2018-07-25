package mx.nic.lab.rpki.api.result.roa;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1StreamParser;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEROctetString;
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

	/**
	 * Supported CMS profiles, used to know how to parse the content
	 */
	private enum CMSProfile {
		ROA, GBR
	}

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
		addKeyValueToBuilder(builder, "id", roa.getId(), true);
		addKeyValueToBuilder(builder, "asn", roa.getAsn(), true);
		addKeyValueToBuilder(builder, "prefix", roa.getPrefixText(), true);
		addKeyValueToBuilder(builder, "prefixLength", roa.getPrefixLength(), true);
		addKeyValueToBuilder(builder, "prefixMaxLength", roa.getPrefixMaxLength(), true);
		addKeyValueToBuilder(builder, "cms", getCmsAsJson(roa.getCmsData(), CMSProfile.ROA), true);
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
			// The id is omitted since is used for internal purposes
			addKeyValueToBuilder(objBuilder, "vcard", gbr.getVcard(), true);
			addKeyValueToBuilder(objBuilder, "cms", getCmsAsJson(gbr.getCmsData(), CMSProfile.GBR), true);
			arrayBuilder.add(objBuilder);
		}
		builder.add("gbr", arrayBuilder);
	}

	/**
	 * Return the CMS data as a {@link JsonObject} (originally received as a byte
	 * array). The parsing is based on RFC 6488, and for each of the supported
	 * objects (ROA and GBR) indicated through the {@link CMSProfile} parse the
	 * content as expected.
	 * 
	 * @param cmsData
	 *            the CMS as a byte array
	 * @param cmsProfile
	 *            CMS profile used to parse the content
	 * @return CMS as a {@link JsonObject}
	 */
	private JsonObject getCmsAsJson(byte[] cmsData, CMSProfile cmsProfile) {
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
		ASN1Encodable encodableContent = sData.getEncapContentInfo().getContent();
		// Each object (ROA, MFT, GBR) defines this content
		if (cmsProfile == CMSProfile.ROA) {
			genericObjectBuilder.add("eContent", getRoaContentAsJson(encodableContent));
		} else if (cmsProfile == CMSProfile.GBR) {
			genericObjectBuilder.add("eContent", getGbrContentAsString(encodableContent));
		}
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
				// Apparently the toString method may work for this value
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

	/**
	 * The ROA content has to be manually parsed using the profile of RFC 6482
	 * 
	 * @param roaContent
	 *            {@link ASN1Encodable} with the EncapContentInfo.Content
	 * @return {link JsonObject} of the ROA Content
	 */
	private JsonObject getRoaContentAsJson(ASN1Encodable roaContent) {
		JsonObjectBuilder roaContentBuilder = Json.createObjectBuilder();
		try {
			ASN1OctetString octetStringRoa = DEROctetString.getInstance(roaContent);
			ASN1StreamParser asn1ParserRoa = new ASN1StreamParser(octetStringRoa.getOctetStream());
			ASN1Sequence asn1SequenceRoa = ASN1Sequence.getInstance(asn1ParserRoa.readObject());
			// Parse backwards, considering that the version isn't explicitly declared
			// 3 elements are expected (RFC 6482 section 3)
			int currentObj = asn1SequenceRoa.size() - 1;

			// ipAddrBlocks
			JsonArrayBuilder ipAddrBlocksBuilder = Json.createArrayBuilder();
			ASN1Sequence ipAddrSeq = ASN1Sequence.getInstance(asn1SequenceRoa.getObjectAt(currentObj));
			// Iterate over ROAIPAddressFamily
			Iterator<ASN1Encodable> ipAddrSeqIterator = ipAddrSeq.iterator();
			while (ipAddrSeqIterator.hasNext()) {
				JsonObjectBuilder ipAddrBlockBuilder = Json.createObjectBuilder();
				ASN1Sequence ipAddrFamSeq = ASN1Sequence.getInstance(ipAddrSeqIterator.next());

				// addressFamily
				ASN1OctetString addressFamily = ASN1OctetString.getInstance(ipAddrFamSeq.getObjectAt(0));
				// The toString method adds a "#", remove it
				ipAddrBlockBuilder.add("addressFamily", addressFamily.toString().replace("#", ""));

				// addresses (another sequence to iterate)
				JsonArrayBuilder addresessBuilder = Json.createArrayBuilder();
				ASN1Sequence addressesSeq = ASN1Sequence.getInstance(ipAddrFamSeq.getObjectAt(1));
				Iterator<ASN1Encodable> addressesIterator = addressesSeq.iterator();
				while (addressesIterator.hasNext()) {
					JsonObjectBuilder addressBuilder = Json.createObjectBuilder();
					ASN1Sequence ipAddressSeq = ASN1Sequence.getInstance(addressesIterator.next());

					// address
					ASN1BitString address = DERBitString.getInstance(ipAddressSeq.getObjectAt(0));
					// The getString method adds a "#", remove it
					addressBuilder.add("address", address.getString().replace("#", ""));

					// maxLenght (Optional)
					if (ipAddressSeq.size() > 1) {
						ASN1Integer maxLength = ASN1Integer.getInstance(ipAddressSeq.getObjectAt(1));
						addressBuilder.add("maxLength", maxLength.getValue());
					}
					addresessBuilder.add(addressBuilder);
				}
				ipAddrBlockBuilder.add("addresses", addresessBuilder);
				ipAddrBlocksBuilder.add(ipAddrBlockBuilder);
			}
			currentObj--;

			// asID
			ASN1Integer asid = ASN1Integer.getInstance(asn1SequenceRoa.getObjectAt(currentObj));
			currentObj--;

			// version
			if (currentObj >= 0) {
				ASN1Integer version = ASN1Integer.getInstance(asn1SequenceRoa.getObjectAt(currentObj));
				roaContentBuilder.add("version", version.getValue());
			} else {
				// Not declared, use default value (RFC 6482 section 3.1)
				roaContentBuilder.add("version", 0);
			}
			
			// And now add the elements "ordered"
			roaContentBuilder.add("asID", asid.getValue());
			roaContentBuilder.add("ipAddrBlocks", ipAddrBlocksBuilder);
		} catch (IOException e) {
			logger.log(Level.WARNING, "The ROA content couldn't be parsed, returning empty object", e);
		}
		return roaContentBuilder.build();
	}

	/**
	 * The GBR content has to be manually parsed using the profile of RFC 6493
	 * 
	 * @param gbrContent
	 *            {@link ASN1Encodable} with the EncapContentInfo.Content
	 * @return <code>String</code> of the GBR Content
	 */
	private String getGbrContentAsString(ASN1Encodable gbrContent) {
		ASN1OctetString octetStringGbr = DEROctetString.getInstance(gbrContent);
		try {
			return new String(octetStringGbr.getOctets(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			logger.log(Level.WARNING,
					"The GBR content couldn't be parsed, returning String representation of the HEX value", e);
			return Strings.fromByteArray(Hex.encode(octetStringGbr.getOctets()));
		}
	}
}
