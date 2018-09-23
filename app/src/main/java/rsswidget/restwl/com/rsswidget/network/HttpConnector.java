package rsswidget.restwl.com.rsswidget.network;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpConnector implements Closeable {

    private int connectionTimeout;
    private URL urlString;

    private static final String HTTP_GET = "GET";

    public static final int DEFAULT_CONNECTION_TIMEOUT = 10000;

    private InputStream inputStreamContent;
    //    private InputStreamReader inputStreamReaderContent;
    private InputStream inputStreamServerError;
//    private InputStreamReader inputStreamReaderServerError;

    public HttpConnector(String connectionStr) throws MalformedURLException {
        this(connectionStr, DEFAULT_CONNECTION_TIMEOUT);
    }

    public HttpConnector(String urlString, int connectionTimeout) throws MalformedURLException {
        this.urlString = new URL(urlString);
        this.connectionTimeout = connectionTimeout;
    }

//    public String sendRequest() throws IOException {
//        HttpURLConnection connection = (HttpURLConnection) urlString.openConnection();
//        connection.setRequestMethod(HTTP_GET);
//        connection.setReadTimeout(connectionTimeout);
//        connection.connect();
//
//        if (connection.getErrorStream() != null) {
//            throw new ConnectException();
//        }
//
//        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
//            StringBuilder buf = new StringBuilder();
//            String line;
//            while ((line = reader.readLine()) != null) {
//                buf.append(line).append("\n");
//            }
//            return buf.toString();
//        }
//    }

    public void sendRequest() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) urlString.openConnection();
        connection.setRequestMethod(HTTP_GET);
        connection.setReadTimeout(connectionTimeout);
        connection.connect();

        if (connection.getErrorStream() != null) {
            inputStreamServerError = connection.getErrorStream();
            throw new ConnectException();
        }
        inputStreamContent = connection.getInputStream();
    }

    public InputStream getInputStreamContent() {
        return inputStreamContent;
    }

    public InputStream getInputStreamServerError() {
        return inputStreamServerError;
    }

    @Override
    public void close() throws IOException {
        if (inputStreamServerError != null) {
            inputStreamServerError.close();
        }
        if (inputStreamContent != null) {
            inputStreamContent.close();
        }
    }
}
