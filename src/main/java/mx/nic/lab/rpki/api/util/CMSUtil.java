package mx.nic.lab.rpki.api.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
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
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Null;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1StreamParser;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.ASN1UTCTime;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.cms.SignerIdentifier;
import org.bouncycastle.asn1.cms.SignerInfo;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.CertificatePolicies;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.NoticeReference;
import org.bouncycastle.asn1.x509.PolicyInformation;
import org.bouncycastle.asn1.x509.PolicyQualifierInfo;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.TBSCertificate;
import org.bouncycastle.asn1.x509.UserNotice;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.encoders.Hex;

/**
 * Utility to get CMS data as JSON objects
 *
 */
public class CMSUtil {

	/**
	 * Supported CMS profiles, used to know how to parse the content
	 */
	public enum CMSProfile {
		ROA, GBR
	}

	private static final Logger logger = Logger.getLogger(CMSUtil.class.getName());

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
	public static JsonObject getCmsAsJson(byte[] cmsData, CMSProfile cmsProfile) {
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
				// Apparently the toString method may work for this value since it's an INTEGER
				// (see RFC 6019)
			} else if (attributeId.equals("1.2.840.113549.1.9.5")) {
				try {
					if (firstAttrValue instanceof ASN1UTCTime) {
						ASN1UTCTime utcTime = ASN1UTCTime.getInstance(firstAttrValue);
						attributeValue = Util.getFormattedDate(utcTime.getAdjustedDate());
					} else if (firstAttrValue instanceof ASN1GeneralizedTime) {
						ASN1GeneralizedTime genTime = ASN1GeneralizedTime.getInstance(firstAttrValue);
						attributeValue = Util.getFormattedDate(genTime.getDate());
					}
				} catch (ParseException e) {
					// If the date wasn's set, then keep the value of toString
				}
			}
			internalTempBuilder.add("attrValues", Json.createArrayBuilder().add(attributeValue));
			internalArrBuilder.add(internalTempBuilder);
		}
		genericObjectBuilder.add("signedAttrs", internalArrBuilder);

		// Based on RFC 5280 y 5754
		JsonObjectBuilder sigAlgBuilder = Json.createObjectBuilder();
		sigAlgBuilder.add("algorithm", signerInfo.getDigestEncryptionAlgorithm().getAlgorithm().getId());
		if (signerInfo.getDigestEncryptionAlgorithm().getParameters() != null) {
			sigAlgBuilder.add("parameters", signerInfo.getDigestEncryptionAlgorithm().getParameters().toString());
		}
		genericObjectBuilder.add("signatureAlgorithm", sigAlgBuilder);

		genericObjectBuilder.add("signature", getBytesAsHexString(signerInfo.getEncryptedDigest().getOctets()));

		genericArrayBuilder.add(genericObjectBuilder);
		contentBuilder.add("signerInfos", genericArrayBuilder);

		cmsBuilder.add("content", contentBuilder);

		return cmsBuilder.build();
	}

	/**
	 * Return the Certificate data as a {@link JsonObject} (originally received as a
	 * byte array). The parsing is based on RFC 6487 and RFC 5280.
	 * 
	 * @param certificateData
	 *            the Certificate as a byte array
	 * @return Certificate as a {@link JsonObject}
	 */
	public static JsonObject getCertAsJson(byte[] certificateData) {
		if (certificateData == null) {
			return null;
		}
		// First, try to parse the CMS
		ASN1InputStream aIn = new ASN1InputStream(new ByteArrayInputStream(certificateData));
		Certificate cert = null;
		try {
			try {
				cert = Certificate.getInstance(aIn.readObject());
			} catch (IOException e) {
				logger.log(Level.WARNING, "The Cert data couldn't be parsed, sending null Cert", e);
				return null;
			}
		} finally {
			if (aIn != null) {
				try {
					aIn.close();
				} catch (IOException e) {
					logger.log(Level.WARNING, "Error closing ASN1InputStream, still sending Cert data in response", e);
				}
			}
		}
		return getCertificateAsJson(cert);
	}

	/**
	 * Return a {@link Certificate} as a {@link JsonObject} based on the rules of
	 * RFC 6487 and RFC 5280
	 * 
	 * @param certificate
	 *            the {@link Certificate} to format
	 * @return The received {@link Certificate} as a {@link JsonObject}
	 */
	private static JsonObject getCertificateAsJson(Certificate certificate) {
		// Profile from RFC 6487
		JsonObjectBuilder builder = Json.createObjectBuilder();

		// Load the TBS certificate based on RFC 5280
		JsonObjectBuilder tbsCertificateBuilder = Json.createObjectBuilder();
		TBSCertificate tbsCertificate = certificate.getTBSCertificate();
		tbsCertificateBuilder.add("version", tbsCertificate.getVersionNumber());
		tbsCertificateBuilder.add("serialNumber", tbsCertificate.getSerialNumber().getValue());
		tbsCertificateBuilder.add("signature", tbsCertificate.getSignature().getAlgorithm().getId());
		tbsCertificateBuilder.add("issuer", tbsCertificate.getIssuer().toString());

		JsonObjectBuilder validityBuilder = Json.createObjectBuilder();
		validityBuilder.add("notBefore", Util.getFormattedDate(tbsCertificate.getStartDate().getDate()));
		validityBuilder.add("notAfter", Util.getFormattedDate(tbsCertificate.getEndDate().getDate()));
		tbsCertificateBuilder.add("validity", validityBuilder);
		tbsCertificateBuilder.add("subject", tbsCertificate.getSubject().toString());

		JsonObjectBuilder spkiBuilder = Json.createObjectBuilder();
		spkiBuilder.add("algorithm", tbsCertificate.getSubjectPublicKeyInfo().getAlgorithm().getAlgorithm().getId());
		spkiBuilder.add("subjectPublicKey",
				getBytesAsHexString(tbsCertificate.getSubjectPublicKeyInfo().getPublicKeyData().getOctets()));
		tbsCertificateBuilder.add("subjectPublicKeyInfo", spkiBuilder);
		// OPTIONAL
		if (tbsCertificate.getIssuerUniqueId() != null) {
			tbsCertificateBuilder.add("issuerUniqueID",
					getBytesAsHexString(tbsCertificate.getIssuerUniqueId().getOctets()));
		}
		// OPTIONAL
		if (tbsCertificate.getSubjectUniqueId() != null) {
			tbsCertificateBuilder.add("subjectUniqueID",
					getBytesAsHexString(tbsCertificate.getSubjectUniqueId().getOctets()));
		}

		JsonArrayBuilder extensionsBuilder = Json.createArrayBuilder();
		Extensions extensions = tbsCertificate.getExtensions();
		ASN1ObjectIdentifier[] oids = extensions.getExtensionOIDs();
		for (ASN1ObjectIdentifier oid : oids) {
			JsonObjectBuilder extensionBuilder = Json.createObjectBuilder();
			Extension extension = extensions.getExtension(oid);
			extensionBuilder.add("extnID", extension.getExtnId().getId());
			extensionBuilder.add("critical", extension.isCritical());
			extensionBuilder.add("extnValue", getCertExtensionValueAsObject(extension));
			extensionsBuilder.add(extensionBuilder);
		}
		tbsCertificateBuilder.add("extensions", extensionsBuilder);

		builder.add("tbsCertificate", tbsCertificateBuilder);

		// Based on RFC 5280 y 5754
		JsonObjectBuilder sigAlgBuilder = Json.createObjectBuilder();
		sigAlgBuilder.add("algorithm", certificate.getSignatureAlgorithm().getAlgorithm().getId());
		if (certificate.getSignatureAlgorithm().getParameters() != null) {
			sigAlgBuilder.add("parameters", certificate.getSignatureAlgorithm().getParameters().toString());
		}
		builder.add("signatureAlgorithm", sigAlgBuilder);
		builder.add("signatureValue", getBytesAsHexString(certificate.getSignature().getOctets()));
		return builder.build();
	}

	/**
	 * Return an {@link Extension} as a {@link JsonObject} following the rules at
	 * "RFC 6487 section 4.8", complemented by "RFC 5280" and "RFC 3779". <br>
	 * The following extensions are NOT EXPECTED (RFC 5280):
	 * <li>4.2.1.5. Policy Mappings (OID 2.5.29.33)
	 * <li>4.2.1.6. Subject Alternative Name (OID 2.5.29.17)
	 * <li>4.2.1.7. Issuer Alternative Name (OID 2.5.29.18)
	 * <li>4.2.1.8. Subject Directory Attributes (OID 2.5.29.9)
	 * <li>4.2.1.10. Name Constraints (OID 2.5.29.30)
	 * <li>4.2.1.11. Policy Constraints (OID 2.5.29.36)
	 * <li>4.2.1.14. Inhibit anyPolicy (OID 2.5.29.54)
	 * <li>4.2.1.15. Freshest CRL (OID 2.5.29.46)
	 * 
	 * @param extension
	 * @return
	 */
	private static JsonStructure getCertExtensionValueAsObject(Extension extension) {
		JsonObjectBuilder extValueBuilder = Json.createObjectBuilder();
		ASN1OctetString octetStringExt = DEROctetString.getInstance(extension.getExtnValue());
		ASN1StreamParser asn1ParserExt = new ASN1StreamParser(octetStringExt.getOctetStream());
		ASN1Encodable asn1EncExt = null;
		try {
			asn1EncExt = asn1ParserExt.readObject();
		} catch (IOException e) {
			logger.log(Level.WARNING,
					"A CMS certificate extension couldn't be parsed, trying to parse the rest. Extension OID: "
							+ extension.getExtnId().getId() + ", value: " + extension.getExtnValue().toString(),
					e);
			return extValueBuilder.build();
		}
		ASN1ObjectIdentifier extOid = extension.getExtnId();
		if (extOid.equals(Extension.basicConstraints)) {
			// 4.8.1. Basic Constraints
			// RFC 5280 4.2.1.9 (OID 2.5.29.19)
			BasicConstraints bc = BasicConstraints.getInstance(asn1EncExt);
			extValueBuilder.add("cA", bc.isCA());
			if (bc.getPathLenConstraint() != null) {
				extValueBuilder.add("pathLenConstraint", bc.getPathLenConstraint());
			}
			return extValueBuilder.build();
		}
		if (extOid.equals(Extension.subjectKeyIdentifier)) {
			// 4.8.2. Subject Key Identifier
			// RFC 5280 4.2.1.2 (OID 2.5.29.14)
			SubjectKeyIdentifier ski = SubjectKeyIdentifier.getInstance(asn1EncExt);
			extValueBuilder.add("keyIdentifier",
					Strings.fromByteArray(Hex.encode(ski.getKeyIdentifier())).toUpperCase());
			return extValueBuilder.build();
		}
		if (extOid.equals(Extension.authorityKeyIdentifier)) {
			// 4.8.3. Authority Key Identifier
			// RFC 5280 4.2.1.1 (OID 2.5.29.35)
			AuthorityKeyIdentifier aki = AuthorityKeyIdentifier.getInstance(asn1EncExt);
			// The authorityCertIssuer and authorityCertSerialNumber fields MUST NOT be
			// present
			if (aki.getKeyIdentifier() != null) {
				extValueBuilder.add("keyIdentifier",
						Strings.fromByteArray(Hex.encode(aki.getKeyIdentifier())).toUpperCase());
			}
			return extValueBuilder.build();
		}
		if (extOid.equals(Extension.keyUsage)) {
			// 4.8.4. Key Usage
			// RFC 5280 4.2.1.3 (OID 2.5.29.15)
			KeyUsage ku = KeyUsage.getInstance(asn1EncExt);
			extValueBuilder.add("digitalSignature", ku.hasUsages(KeyUsage.digitalSignature));
			extValueBuilder.add("nonRepudiation", ku.hasUsages(KeyUsage.nonRepudiation));
			extValueBuilder.add("keyEncipherment", ku.hasUsages(KeyUsage.keyEncipherment));
			extValueBuilder.add("dataEncipherment", ku.hasUsages(KeyUsage.dataEncipherment));
			extValueBuilder.add("keyAgreement", ku.hasUsages(KeyUsage.keyAgreement));
			extValueBuilder.add("keyCertSign", ku.hasUsages(KeyUsage.keyCertSign));
			extValueBuilder.add("cRLSign", ku.hasUsages(KeyUsage.cRLSign));
			extValueBuilder.add("encipherOnly", ku.hasUsages(KeyUsage.encipherOnly));
			extValueBuilder.add("decipherOnly", ku.hasUsages(KeyUsage.decipherOnly));
			return extValueBuilder.build();
		}
		if (extOid.equals(Extension.extendedKeyUsage)) {
			// 4.8.5. Extended Key Usage
			// RFC 5280 4.2.1.12 (OID 2.5.29.37)
			// Probably it won't be present, still leave the support
			ExtendedKeyUsage eku = ExtendedKeyUsage.getInstance(asn1EncExt);
			JsonArrayBuilder purposeBuilder = Json.createArrayBuilder();
			for (KeyPurposeId purposeId : eku.getUsages()) {
				purposeBuilder.add(purposeId.getId());
			}
			return purposeBuilder.build();
		}
		if (extOid.equals(Extension.cRLDistributionPoints)) {
			// 4.8.6. CRL Distribution Points
			// RFC 5280 4.2.1.13 (OID 2.5.29.31)
			CRLDistPoint cdp = CRLDistPoint.getInstance(asn1EncExt);
			JsonArrayBuilder distPointsBuilder = Json.createArrayBuilder();
			// The CRLIssuer field MUST be omitted, and the distributionPoint field MUST be
			// present. The Reasons field MUST be omitted.
			if (cdp.getDistributionPoints() != null) {
				// The distributionPoint MUST contain the fullName field, and MUST NOT contain a
				// nameRelativeToCRLIssuer
				for (DistributionPoint dp : cdp.getDistributionPoints()) {
					JsonObjectBuilder distPointBuilder = Json.createObjectBuilder();
					JsonObjectBuilder distPointNameBuilder = Json.createObjectBuilder();
					// The "fullName" is a GeneralNames sequence
					GeneralNames gns = GeneralNames.getInstance(dp.getDistributionPoint().getName());
					JsonArrayBuilder gnsBuilder = Json.createArrayBuilder();
					for (GeneralName gn : gns.getNames()) {
						gnsBuilder.add(gn.getName().toString());
					}
					distPointNameBuilder.add("fullName", gnsBuilder);
					distPointBuilder.add("distributionPoint", distPointNameBuilder);
					distPointsBuilder.add(distPointBuilder);
				}
			}
			return distPointsBuilder.build();
		}
		if (extOid.equals(Extension.authorityInfoAccess)) {
			// 4.8.7. Authority Information Access
			// RFC 5280 4.2.2.1 (OID 1.3.6.1.5.5.7.1.1)
			AuthorityInformationAccess aia = AuthorityInformationAccess.getInstance(asn1EncExt);
			JsonArrayBuilder accessDescriptionsBuilder = Json.createArrayBuilder();
			for (AccessDescription ad : aia.getAccessDescriptions()) {
				JsonObjectBuilder accessDescriptionBuilder = Json.createObjectBuilder();
				accessDescriptionBuilder.add("accessMethod", ad.getAccessMethod().getId());
				accessDescriptionBuilder.add("accessLocation", ad.getAccessLocation().getName().toString());
				accessDescriptionsBuilder.add(accessDescriptionBuilder);
			}
			return accessDescriptionsBuilder.build();
		}
		if (extOid.equals(Extension.subjectInfoAccess)) {
			// 4.8.8. Subject Information Access
			// RFC 5280 4.2.2.2 (OID 1.3.6.1.5.5.7.1.11)
			AuthorityInformationAccess sia = AuthorityInformationAccess.getInstance(asn1EncExt);
			JsonArrayBuilder subjectDescriptionsBuilder = Json.createArrayBuilder();
			for (AccessDescription ad : sia.getAccessDescriptions()) {
				JsonObjectBuilder accessDescriptionBuilder = Json.createObjectBuilder();
				accessDescriptionBuilder.add("accessMethod", ad.getAccessMethod().getId());
				accessDescriptionBuilder.add("accessLocation", ad.getAccessLocation().getName().toString());
				subjectDescriptionsBuilder.add(accessDescriptionBuilder);
			}
			return subjectDescriptionsBuilder.build();
		}
		if (extOid.equals(Extension.certificatePolicies)) {
			// 4.8.9. Certificate Policies
			// RFC 5280 4.2.1.4 (OID 2.5.29.32)
			CertificatePolicies cp = CertificatePolicies.getInstance(asn1EncExt);
			JsonArrayBuilder certificatePoliciesBuilder = Json.createArrayBuilder();
			// It MUST include exactly one policy, as specified in the RPKI CP [RFC6484]
			for (PolicyInformation pi : cp.getPolicyInformation()) {
				JsonObjectBuilder piBuilder = Json.createObjectBuilder();
				piBuilder.add("policyIdentifier", pi.getPolicyIdentifier().getId());
				// OPTIONAL
				JsonArrayBuilder pqisBuilder = Json.createArrayBuilder();
				if (pi.getPolicyQualifiers() != null) {
					Iterator<ASN1Encodable> pqIterator = pi.getPolicyQualifiers().iterator();
					while (pqIterator.hasNext()) {
						JsonObjectBuilder pqiBuilder = Json.createObjectBuilder();
						PolicyQualifierInfo pqi = PolicyQualifierInfo.getInstance(pqIterator.next());
						pqiBuilder.add("policyQualifierId", pqi.getPolicyQualifierId().getId());
						ASN1Encodable qualifier = pqi.getQualifier();
						if (qualifier instanceof DERIA5String) {
							// It's a CPSuri
							pqiBuilder.add("qualifier", qualifier.toString());
						} else {
							// It's a UserNotice
							UserNotice un = UserNotice.getInstance(qualifier);
							JsonObjectBuilder unBuilder = Json.createObjectBuilder();
							if (un.getNoticeRef() != null) {
								JsonObjectBuilder nrBuilder = Json.createObjectBuilder();
								NoticeReference nr = un.getNoticeRef();
								nrBuilder.add("organization", nr.getOrganization().toString());
								JsonArrayBuilder nnBuilder = Json.createArrayBuilder();
								for (ASN1Integer number : nr.getNoticeNumbers()) {
									nnBuilder.add(number.getValue());
								}
								nrBuilder.add("noticeNumbers", nnBuilder);
								unBuilder.add("noticeRef", nrBuilder);
							}
							if (un.getExplicitText() != null) {
								unBuilder.add("explicitText", un.getExplicitText().toString());
							}
							pqiBuilder.add("qualifier", unBuilder);
						}
						pqisBuilder.add(pqiBuilder);
					}
				}
				piBuilder.add("policyQualifiers", pqisBuilder);
				certificatePoliciesBuilder.add(piBuilder);
			}
			return certificatePoliciesBuilder.build();
		}
		if (extOid.equals(new ASN1ObjectIdentifier("1.3.6.1.5.5.7.1.7").intern())) {
			// 4.8.10. IP Resources
			// RFC 3779 2.2.3 (OID 1.3.6.1.5.5.7.1.7)
			ASN1Sequence addressBlocksSequence = ASN1Sequence.getInstance(asn1EncExt);
			JsonArrayBuilder ipAddrBlocksBuilder = Json.createArrayBuilder();
			Iterator<ASN1Encodable> addressBlocksIterator = addressBlocksSequence.iterator();
			while (addressBlocksIterator.hasNext()) {
				ASN1Sequence addrBlockFam = ASN1Sequence.getInstance(addressBlocksIterator.next());
				JsonObjectBuilder addrBlockFamBuilder = Json.createObjectBuilder();

				// addressFamily
				ASN1OctetString addressFamily = ASN1OctetString.getInstance(addrBlockFam.getObjectAt(0));
				addrBlockFamBuilder.add("addressFamily", getBytesAsHexString(addressFamily.getOctets()));

				// ipAddressChoice
				JsonObjectBuilder addressChoiceBuilder = Json.createObjectBuilder();
				if (addrBlockFam.getObjectAt(1) instanceof ASN1Null) {
					addressChoiceBuilder.add("inherit", "NULL");
				} else {
					// It's a sequence of IPAddressOrRange
					ASN1Sequence addrOrRange = ASN1Sequence.getInstance(addrBlockFam.getObjectAt(1));
					JsonArrayBuilder addrOrRangesBuilder = Json.createArrayBuilder();
					Iterator<ASN1Encodable> addrOrRangeIterator = addrOrRange.iterator();
					while (addrOrRangeIterator.hasNext()) {
						ASN1Encodable addrOrRangeEnc = addrOrRangeIterator.next();
						JsonObjectBuilder addrOrRangeBuilder = Json.createObjectBuilder();
						if (addrOrRangeEnc instanceof ASN1BitString) {
							// It's an addressPrefix
							ASN1BitString addressPrefix = DERBitString.getInstance(addrOrRangeEnc);
							addrOrRangeBuilder.add("addressPrefix", getBytesAsHexString(addressPrefix.getBytes()));
						} else {
							// It's an addressRange sequence
							ASN1Sequence addrRange = ASN1Sequence.getInstance(addrOrRangeEnc);
							JsonObjectBuilder rangeBuilder = Json.createObjectBuilder();
							// min
							ASN1BitString min = DERBitString.getInstance(addrRange.getObjectAt(0));
							rangeBuilder.add("min", getBytesAsHexString(min.getBytes()));
							// max
							ASN1BitString max = DERBitString.getInstance(addrRange.getObjectAt(1));
							rangeBuilder.add("max", getBytesAsHexString(max.getBytes()));
							addrOrRangeBuilder.add("addressRange", rangeBuilder);
						}
						addrOrRangesBuilder.add(addrOrRangeBuilder);
					}
					addressChoiceBuilder.add("addressesOrRanges", addrOrRangesBuilder);
				}
				addrBlockFamBuilder.add("ipAddressChoice", addressChoiceBuilder);
				ipAddrBlocksBuilder.add(addrBlockFamBuilder);
			}
			return ipAddrBlocksBuilder.build();
		}
		if (extOid.equals(new ASN1ObjectIdentifier("1.3.6.1.5.5.7.1.8").intern())) {
			// 4.8.11. AS Resources
			// RFC 3779 3.2.3 (OID 1.3.6.1.5.5.7.1.8)
			ASN1Sequence asIdsSequence = ASN1Sequence.getInstance(asn1EncExt);
			// Routing Domain Identifier (RDI) values are NOT supported in this profile and
			// MUST NOT be used.
			if (asIdsSequence.getObjectAt(0) != null) {
				ASN1TaggedObject asIdChoiceTag = ASN1TaggedObject.getInstance(asIdsSequence.getObjectAt(0));
				JsonObjectBuilder asIdChoiceBuilder = Json.createObjectBuilder();
				if (asIdChoiceTag.getObject() instanceof ASN1Null) {
					asIdChoiceBuilder.add("inherit", "NULL");
				} else {
					// It's an ASIdOrRange sequence
					ASN1Sequence asIdOrRangeSeq = ASN1Sequence.getInstance(asIdChoiceTag.getObject());
					JsonArrayBuilder asIdOrRangesBuilder = Json.createArrayBuilder();
					Iterator<ASN1Encodable> asIdOrRangeIterator = asIdOrRangeSeq.iterator();
					while (asIdOrRangeIterator.hasNext()) {
						ASN1Encodable asIdOrRangeEnc = asIdOrRangeIterator.next();
						JsonObjectBuilder asIdOrRangeBuilder = Json.createObjectBuilder();
						if (asIdOrRangeEnc instanceof ASN1Integer) {
							// It's an id
							ASN1Integer addressPrefix = ASN1Integer.getInstance(asIdOrRangeEnc);
							asIdOrRangeBuilder.add("id", addressPrefix.getValue());
						} else {
							// It's an ASRange sequence
							ASN1Sequence asRange = ASN1Sequence.getInstance(asIdOrRangeEnc);
							JsonObjectBuilder rangeBuilder = Json.createObjectBuilder();
							// min
							ASN1Integer min = ASN1Integer.getInstance(asRange.getObjectAt(0));
							rangeBuilder.add("min", min.getValue());
							// max
							ASN1Integer max = ASN1Integer.getInstance(asRange.getObjectAt(1));
							rangeBuilder.add("max", max.getValue());
							asIdOrRangeBuilder.add("range", rangeBuilder);
						}
						asIdOrRangesBuilder.add(asIdOrRangeBuilder);
					}
					asIdChoiceBuilder.add("asIdsOrRanges", asIdOrRangesBuilder);
				}
				extValueBuilder.add("asnum", asIdChoiceBuilder);
			}
			return extValueBuilder.build();
		}
		// Nothing matched, return empty object
		return extValueBuilder.build();
	}

	/**
	 * The ROA content has to be manually parsed using the profile of RFC 6482
	 * 
	 * @param roaContent
	 *            {@link ASN1Encodable} with the EncapContentInfo.Content
	 * @return {link JsonObject} of the ROA Content
	 */
	private static JsonObject getRoaContentAsJson(ASN1Encodable roaContent) {
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
				ipAddrBlockBuilder.add("addressFamily", getBytesAsHexString(addressFamily.getOctets()));

				// addresses (another sequence to iterate)
				JsonArrayBuilder addresessBuilder = Json.createArrayBuilder();
				ASN1Sequence addressesSeq = ASN1Sequence.getInstance(ipAddrFamSeq.getObjectAt(1));
				Iterator<ASN1Encodable> addressesIterator = addressesSeq.iterator();
				while (addressesIterator.hasNext()) {
					JsonObjectBuilder addressBuilder = Json.createObjectBuilder();
					ASN1Sequence ipAddressSeq = ASN1Sequence.getInstance(addressesIterator.next());

					// address
					ASN1BitString address = DERBitString.getInstance(ipAddressSeq.getObjectAt(0));
					addressBuilder.add("address", getBytesAsHexString(address.getBytes()));

					// maxLength (Optional)
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
	private static String getGbrContentAsString(ASN1Encodable gbrContent) {
		ASN1OctetString octetStringGbr = DEROctetString.getInstance(gbrContent);
		try {
			return new String(octetStringGbr.getOctets(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			logger.log(Level.WARNING,
					"The GBR content couldn't be parsed, returning String representation of the HEX value", e);
			return Strings.fromByteArray(Hex.encode(octetStringGbr.getOctets()));
		}
	}

	/**
	 * Get the byte array as HEX String representation
	 * 
	 * @param bytes
	 * @return
	 */
	private static String getBytesAsHexString(byte[] bytes) {
		return Strings.fromByteArray(Hex.encode(bytes)).toUpperCase();
	}

	/**
	 * Check if the encoded data is a valid {@link SubjectKeyIdentifier}
	 * 
	 * @param encodedData
	 *            data to validate
	 * @return <code>true</code> if an instance of {@link SubjectKeyIdentifier}
	 *         could be created (this is the main validation)
	 */
	public static boolean isValidSubjectKeyIdentifier(byte[] encodedData) {
		try {
			SubjectKeyIdentifier skiInfo = SubjectKeyIdentifier.getInstance(encodedData);
			if (skiInfo == null) {
				return false;
			}
		} catch (IllegalArgumentException e) {
			return false;
		}
		return true;
	}

	/**
	 * Check if the encoded data is a valid {@link SubjectPublicKeyInfo}
	 * 
	 * @param encodedData
	 *            data to validate
	 * @return <code>true</code> if an instance of {@link SubjectPublicKeyInfo}
	 *         could be created (this is the main validation)
	 */
	public static boolean isValidSubjectPublicKey(byte[] encodedData) {
		try {
			SubjectPublicKeyInfo pkInfo = SubjectPublicKeyInfo.getInstance(encodedData);
			if (pkInfo == null) {
				return false;
			}
		} catch (IllegalArgumentException e) {
			return false;
		}
		return true;
	}
}
