package info.linuxehacking.iotmanager;

import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.CertificateEncodingException;

/**
 * Created by tiziano on 07/03/15.
 */
public class SHA1VerifyGenerator {
    public static SSLSocketFactory generateFactory(final String trustedsha1) throws NoSuchAlgorithmException, KeyManagementException {
        final TrustManager[] trustSHA1 = new TrustManager[] { new X509TrustManager() {
            @Override
            public void checkClientTrusted( final X509Certificate[] chain, final String authType ) throws CertificateException {
                for ( int i = 0; i < chain.length; i++ )
                {
                    try {
                        if ( getThumbPrint(chain[i]).equals(trustedsha1) )
                        {
                            return;
                        }
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (CertificateEncodingException e) {
                        e.printStackTrace();
                    } catch (java.security.cert.CertificateEncodingException e) {
                        e.printStackTrace();
                    }
                }
                throw new CertificateException("SHA Fingerprint not matching");
            }
            @Override
            public void checkServerTrusted( final X509Certificate[] chain, final String authType ) throws CertificateException {
                for ( int i = 0; i < chain.length; i++ )
                {
                    try {
                        if ( getThumbPrint(chain[i]).equals(trustedsha1) )
                        {
                            return;
                        }
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (CertificateEncodingException e) {
                        e.printStackTrace();
                    } catch (java.security.cert.CertificateEncodingException e) {
                        e.printStackTrace();
                    }
                }
                throw new CertificateException("SHA Fingerprint not matching");
            }
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        } };

        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init( null, trustSHA1, new java.security.SecureRandom() );
        // Create an ssl socket factory with our all-trusting manager
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        return sslSocketFactory;
    }

    public static String getThumbPrint(X509Certificate cert)
            throws NoSuchAlgorithmException, CertificateEncodingException, java.security.cert.CertificateEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] der = cert.getEncoded();
        md.update(der);
        byte[] digest = md.digest();
        return hexify(digest);

    }

    public static String hexify (byte bytes[]) {

        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        StringBuffer buf = new StringBuffer(bytes.length * 2);

        for (int i = 0; i < bytes.length; ++i) {
            buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
            buf.append(hexDigits[bytes[i] & 0x0f]);
        }

        return buf.toString();
    }


    public static HostnameVerifier getNullVerifier()
    {
        return new HostnameVerifier() {
            @Override
            public boolean verify(String hostname2, SSLSession session) {
                return true;
            }
        };
    }
}
