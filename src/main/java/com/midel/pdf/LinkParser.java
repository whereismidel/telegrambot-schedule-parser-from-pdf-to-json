package com.midel.pdf;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LinkParser {
    public static List<String> parseLinks(String sourceURL, String startWithPattern) {
        List<String> linksList = new ArrayList<>();

        try {
            Document document = Jsoup.connect(sourceURL).get();

            Elements links = document.select("a");

            for (Element link : links) {
                String href = link.attr("href");
                if (href.startsWith(startWithPattern)) {
                    linksList.add(href);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return linksList;
    }
}