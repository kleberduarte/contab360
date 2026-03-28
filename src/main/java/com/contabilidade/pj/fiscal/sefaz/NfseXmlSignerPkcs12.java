package com.contabilidade.pj.fiscal.sefaz;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import org.w3c.dom.Element;

final class NfseXmlSignerPkcs12 {

    private NfseXmlSignerPkcs12() {
    }

    static void assinarInfRps(Element infRps, String pkcs12Path, String senha, boolean usarSha1) throws Exception {
        if (senha == null) {
            senha = "";
        }
        char[] pwd = senha.toCharArray();
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(pkcs12Path)) {
            ks.load(fis, pwd);
        }
        String alias = ks.aliases().nextElement();
        PrivateKey pk = (PrivateKey) ks.getKey(alias, pwd);
        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
        if (pk == null || cert == null) {
            throw new IllegalStateException("Chave privada ou certificado nao encontrados no PKCS12.");
        }

        String id = infRps.getAttribute("Id");
        if (id == null || id.isBlank()) {
            throw new IllegalStateException("InfRps sem atributo Id para assinatura.");
        }
        infRps.setIdAttribute("Id", true);

        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
        Transform enveloped = fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null);
        Reference ref = fac.newReference(
                "#" + id,
                fac.newDigestMethod(usarSha1 ? DigestMethod.SHA1 : DigestMethod.SHA256, null),
                Collections.singletonList(enveloped),
                null,
                null
        );
        SignedInfo si = fac.newSignedInfo(
                fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                fac.newSignatureMethod(
                        usarSha1 ? SignatureMethod.RSA_SHA1 : SignatureMethod.RSA_SHA256,
                        null
                ),
                Collections.singletonList(ref)
        );

        KeyInfoFactory kif = fac.getKeyInfoFactory();
        List<Object> x509Content = new ArrayList<>();
        x509Content.add(cert);
        X509Data xd = kif.newX509Data(x509Content);
        KeyInfo ki = kif.newKeyInfo(Collections.singletonList(xd));

        XMLSignature signature = fac.newXMLSignature(si, ki);
        DOMSignContext dsc = new DOMSignContext(pk, infRps);
        signature.sign(dsc);
    }
}
