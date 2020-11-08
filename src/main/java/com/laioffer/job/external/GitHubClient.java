package com.laioffer.job.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laioffer.job.entity.Item;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

public class GitHubClient {
    private static final String URL_TEMPLATE = "https://jobs.github.com/positions.json?description=%s&lat=%s&long=%s";
    private static final String DEFAULT_KEYWORD = "developer";

    public List<Item> search(double lat, double lon, String keyword) {
        if (keyword == null) {
            keyword = DEFAULT_KEYWORD;
        }


        // “hello world” => “hello%20world”
        try {
            keyword = URLEncoder.encode(keyword, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String url = String.format(URL_TEMPLATE, keyword, lat, lon);

        CloseableHttpClient httpclient = HttpClients.createDefault();

        // Create a custom response handler
        ResponseHandler<List<Item>> responseHandler = new ResponseHandler<List<Item>>() {
            @Override
            public List<Item> handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                if (response.getStatusLine().getStatusCode() != 200) {
                    return Collections.emptyList();
                }
                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    return Collections.emptyList();
                }
                ObjectMapper mapper = new ObjectMapper();
//                return Arrays.asList(mapper.readValue(entity.getContent(), Item[].class));
                List<Item> items = Arrays.asList(mapper.readValue(entity.getContent(), Item[].class));
                extractKeywords(items);
                return items;

            }
        };

        try {
            return httpclient.execute(new HttpGet(url), responseHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    private void extractKeywords(List<Item> items) {
        MonkeyLearnClient monkeyLearnClient = new MonkeyLearnClient();

        if(items.isEmpty()){
            System.out.println("items empty....");
            return;
        }
        List<String> descriptions = new ArrayList<>();
        for (Item item : items) {
            descriptions.add(item.getDescription());
        }

        List<String> titles = new ArrayList<>();
        for (Item item : items) {
            titles.add(item.getTitle());
        }
        // Java 8 stream API
//        List<String> descriptions = items.stream()
//                .map(Item::getDescription)
//                .collect(Collectors.toList());

        List<Set<String>> keywordList = monkeyLearnClient.extract(descriptions);
        if (keywordList.isEmpty()) {
            System.out.println("extract titles");
            keywordList = monkeyLearnClient.extract(titles);
//            System.out.println("keyword list empty...." + "  items:" + items);
//            return;
        }
        System.out.println("keywordList:" + keywordList);
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setKeywords(keywordList.get(i));
        }
    }



}