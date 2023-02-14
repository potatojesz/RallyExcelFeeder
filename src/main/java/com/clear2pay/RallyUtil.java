package com.clear2pay;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;

import com.clear2pay.model.RallyItem;

public class RallyUtil {
    private String url;
    private String apiKey;

    public RallyUtil() {
        try (InputStream input = Files.newInputStream(Paths.get("src/main/resources/settings.properties"))) {
            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            url = prop.getProperty("rally.url");
            apiKey = prop.getProperty("rally.apikey");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void feed(Map<String, RallyItem> rallyItems) throws IOException {
        List<String> epicList = new ArrayList<>();
        List<String> featureList = new ArrayList<>();
        List<String> defectList = new ArrayList<>();
        List<String> userStoryList = new ArrayList<>();
        List<String> tasktList = new ArrayList<>();
        for(String ticket : rallyItems.keySet()){
            if(ticket.startsWith("E")) {
                epicList.add(ticket);
            } else if(ticket.startsWith("F")) {
                featureList.add(ticket);
            } else if (ticket.startsWith("DE")) {
                defectList.add(ticket);
            } else if (ticket.startsWith("US")) {
                userStoryList.add(ticket);
            } else if (ticket.startsWith("TA")) {
                tasktList.add(ticket);
            }
        }

        Map<String, RallyItem> epicMap = getDifferentTypeMap(epicList, "portfolioitem/epic", new String[]{"FormattedID", "State", "Name", "_ref", "_type"});
        Map<String, RallyItem> featureMap = getDifferentTypeMap(featureList, "portfolioitem/feature", new String[]{"FormattedID", "State", "Name", "_ref", "_type"});
        Map<String, RallyItem> defectMap = getDifferentTypeMap(defectList, "defect", new String[]{"FormattedID", "State", "Name", "_ref", "_type"});
        Map<String, RallyItem> userStoryMap = getDifferentTypeMap(userStoryList, "hierarchicalrequirement", new String[]{"FormattedID", "ScheduleState", "Name", "_ref", "_type"});
        Map<String, RallyItem> taskMap = getDifferentTypeMap(tasktList, "task", new String[]{"FormattedID", "State", "Name", "_ref", "_type"});

        rallyItems.putAll(epicMap);
        rallyItems.putAll(featureMap);
        rallyItems.putAll(defectMap);
        rallyItems.putAll(userStoryMap);
        rallyItems.putAll(taskMap);
    }

    protected Map<String, RallyItem> getDifferentTypeMap(List<String> tickets, String type, String[] fetchArgs) throws IOException {
        Map<String, RallyItem> typeMap = new HashMap<>();
        try (RallyRestApi restApi = createRestApi()) {
            if (!tickets.isEmpty() && tickets != null) {
                QueryRequest typeRequest = new QueryRequest(type);
                typeMap = getRallyTicketTypeMap(tickets, restApi, typeRequest, fetchArgs, type);
            }
        }
        return typeMap;
    }

    protected RallyRestApi createRestApi() {
        RallyRestApi restApi = new RallyRestApi(URI.create(url), apiKey);
        allowSSL(restApi);

        return restApi;
    }

    protected Map<String, RallyItem> getRallyTicketTypeMap(List<String> tickets, RallyRestApi restApi, QueryRequest queryRequest, String[] fetchArgs, String type) throws IOException {
        //queryRequest.setFetch(new Fetch(fetchArgs));
        queryRequest.setQueryFilter(new QueryFilter("FormattedID", "in", String.join(",", tickets)));
        QueryResponse queryResponse = restApi.query(queryRequest);
        if (queryResponse.wasSuccessful()) {
            Map<String, RallyItem> ticketTypeMap = new HashMap<>(queryResponse.getTotalResultCount());
            JsonArray result = queryResponse.getResults();
            result.forEach(m -> ticketTypeMap.put(m.getAsJsonObject().get("FormattedID").getAsString(),
                    new RallyItem(m.getAsJsonObject().get("FormattedID").getAsString(), m)));
            return ticketTypeMap;
        } else {
            throw new RuntimeException(String.join(" ", queryResponse.getErrors()));
        }
    }

    protected void allowSSL(RallyRestApi restApi) {
        HttpClient client = restApi.getClient();
        //noinspection deprecation
        client.getConnectionManager().getSchemeRegistry().unregister("https");
        //noinspection deprecation
        client.getConnectionManager().getSchemeRegistry().register(new Scheme("https", 443, new SSLSocketFactory(createAlwaysTrustSSLContext(), new X509HostnameVerifier() {
            @Override
            public void verify(String s, SSLSocket sslSocket) throws IOException {

            }

            @Override
            public void verify(String s, X509Certificate x509Certificate) throws SSLException {

            }

            @Override
            public void verify(String s, String[] strings, String[] strings1) throws SSLException {

            }

            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        })));
    }

    private static SSLContext createAlwaysTrustSSLContext() {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");

            try {
                sslContext.init((KeyManager[])null, new TrustManager[]{new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String authType) throws CertificateException {
                    }

                    public void checkServerTrusted(X509Certificate[] x509Certificates, String authType) throws CertificateException {
                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                }}, new SecureRandom());
                return sslContext;
            } catch (KeyManagementException var2) {
                throw new RuntimeException(var2);
            }
        } catch (NoSuchAlgorithmException var3) {
            throw new RuntimeException(var3);
        }
    }
}
