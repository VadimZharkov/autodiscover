import org.apache.commons.codec.binary.Base64;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(final String[] args) throws Exception {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "DEBUG");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire", "ERROR");

        String emailAddress = "";
        String password = "";
        String domain = "";

        autodiscover(domain, emailAddress, password);
    }

    private static void autodiscover(String domain, String username, String password)
            throws IOException, ProtocolException, URISyntaxException {
         try (CloseableHttpClient httpClient = HttpClients.custom()
                .disableRedirectHandling()
                .build()) {
            autodiscover(httpClient, "https://" + domain + "/autodiscover/autodiscover.xml", username, password);
            autodiscover(httpClient, "https://autodiscover." + domain + "/autodiscover/autodiscover.xml", username, password);
            redirect(httpClient, "http://autodiscover." + domain + "/autodiscover/autodiscover.xml", username, password);
        }
    }

    private static void autodiscover(CloseableHttpClient httpClient, String url, String username, String password)
            throws ParseException, URISyntaxException, IOException {
        HttpPost request = makePostRequest(url, username, password);
        printPostRequest(request);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
            printResponse(response, responseString);
        } catch (IOException e) {
            System.out.println("Error sending request");
        }
    }

    private static void redirect(CloseableHttpClient httpClient, String url, String username, String password)
            throws URISyntaxException, ProtocolException {
        HttpGet request = makeGetRequest(url, username, password);
        printGetRequest(request);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
            printResponse(response, responseString);
            if (response.getCode() == 302) {
                String location = response.getFirstHeader("Location").getValue();
                System.out.println("Location: " + location);
                autodiscover(httpClient, location, username, password);
            }
        } catch (IOException | ParseException e) {
            System.out.println("Error sending request");
        }
    }

    private static HttpPost makePostRequest(String url, String username, String password) {
        HttpPost request = new HttpPost(url);
        request.setHeader(HttpHeaders.CONTENT_TYPE, "text/xml; charset=utf-8");
        request.setHeader(HttpHeaders.AUTHORIZATION, makeAuthHeader(username, password));

        String requestBody = createAutodiscoverXml(username);
        request.setEntity(new StringEntity(requestBody));

        return request;
    }

    private static HttpGet makeGetRequest(String url, String username, String password) {
        HttpGet request = new HttpGet(url);
        return request;
    }

    private static String makeAuthHeader(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));
        String authHeader = "Basic " + new String(encodedAuth);

        return authHeader;
    }

    private static void printPostRequest(HttpPost request) throws IOException, ParseException, URISyntaxException {
        Header[] headers = request.getHeaders();
        String content = EntityUtils.toString(request.getEntity());

        System.out.println("-----------------------------------------------------");
        System.out.println(request.getMethod() + " " + request.getUri().toString());
        for (Header header : headers) {
            System.out.println(header.getName() + ": " + header.getValue());
        }
        System.out.println(content);
        System.out.println();
    }

    private static void printGetRequest(HttpGet request) throws URISyntaxException {
        Header[] headers = request.getHeaders();

        System.out.println("-----------------------------------------------------");
        System.out.println(request.getMethod() + " " + request.getUri().toString());
        for (Header header : headers) {
            System.out.println(header.getName() + ": " + header.getValue());
        }
        System.out.println();
    }

    private static void printResponse(CloseableHttpResponse response, String responseString) {
        Header[] headers = response.getHeaders();
        System.out.println(response.getCode() + " " + response.getReasonPhrase());
        for (Header header : headers) {
            System.out.println(header.getName() + ": " + header.getValue());
        }
        System.out.println(responseString);
    }

    private static String createAutodiscoverXml(String emailAddress) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        sb.append("<Autodiscover xmlns=\"http://schemas.microsoft.com/exchange/autodiscover/mobilesync/requestschema/2006\">");
        sb.append("<Request>");
        sb.append("<EMailAddress>").append(emailAddress).append("</EMailAddress>");
        sb.append("<AcceptableResponseSchema>http://schemas.microsoft.com/exchange/autodiscover/mobilesync/responseschema/2006</AcceptableResponseSchema>");
        sb.append("</Request>");
        sb.append("</Autodiscover>");

        return sb.toString();
    }
}
