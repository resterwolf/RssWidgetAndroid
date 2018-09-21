package rsswidget.restwl.com.rsswidget.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpConnector {

    private int connectionTimeout;
    private URL rssConnectionUrl;

    private static final String HTTP_GET = "GET";

    public static final int DEFAULT_CONNECTION_TIMEOUT = 10000;

    public HttpConnector(String connectionStr) throws MalformedURLException {
        this(connectionStr, DEFAULT_CONNECTION_TIMEOUT);
    }

    public HttpConnector(String connectionStr, int connectionTimeout) throws MalformedURLException {
        this.rssConnectionUrl = new URL(connectionStr);
        this.connectionTimeout = connectionTimeout;
        trustAllHosts();
    }

    public String getContent() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) rssConnectionUrl.openConnection();
        connection.setRequestMethod(HTTP_GET);
        connection.setReadTimeout(connectionTimeout);
        connection.connect();

        if (connection.getErrorStream() != null) {
            throw new ConnectException();
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder buf = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                buf.append(line).append("\n");
            }
            return buf.toString();
        }
    }

    public InputStream getContentStream() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) rssConnectionUrl.openConnection();
        connection.setRequestMethod(HTTP_GET);
        connection.setReadTimeout(connectionTimeout);
        connection.connect();

        if (connection.getErrorStream() != null) {
            throw new ConnectException();
        }
        return connection.getInputStream();
    }

    private static void trustAllHosts() {

        X509TrustManager easyTrustManager = new X509TrustManager() {

            public void checkClientTrusted(
                    X509Certificate[] chain,
                    String authType) throws CertificateException {
                // Oh, I am easy!
            }

            public void checkServerTrusted(
                    X509Certificate[] chain,
                    String authType) throws CertificateException {
                // Oh, I am easy!
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

        };

        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {easyTrustManager};

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");

            sc.init(null, trustAllCerts, new java.security.SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
