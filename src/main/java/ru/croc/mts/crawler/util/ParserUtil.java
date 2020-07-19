package ru.croc.mts.crawler.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.croc.mts.crawler.task.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ParserUtil {

    private Logger logger = Logger.getLogger(ParserUtil.class.getName());

    private Document doc;

    public ParserUtil(String source){
        doc = Jsoup.parseBodyFragment(source);
    }

    public List<String> getAllUrls(){
        List<String> result = new ArrayList<String>();
        Elements elements = doc.getElementsByTag("h3");
        if (!elements.isEmpty()) {
            for (int i=0; i<elements.size(); i++) {
                Element elm = elements.get(i);
                if (elm.hasClass("video-title")) {
                    Element parent = elm.parent();
                    if ("a".equalsIgnoreCase(parent.tagName())) {
                        String url = parent.attr("href");
                        if (url!=null) {
                            result.add(url);
                        }
                    }
                }
            }
        }
        return result;
    }

    public Result parsePage(){
        Result result = new Result();
        Elements metas = doc.getElementsByTag("meta");
        if (!metas.isEmpty()) {
            for (int i=0; i<metas.size(); i++) {
                Element meta = metas.get(i);
                String propName = meta.attr("property");
                String propValue = meta.attr("content");
                if ("ya:ovs:content_id".equalsIgnoreCase(propName)) {
                    result.setId(propValue);
                } else if ("og:title".equalsIgnoreCase(propName)) {
                    result.setTitleRussian(propValue);
                } else if ("ya:ovs:country".equalsIgnoreCase(propName)) {
                    result.setCountry(propValue);
                } else if ("ya:ovs:genre".equalsIgnoreCase(propName)) {
                    result.setGenre(propValue);
                } else if ("ya:ovs:rating".equalsIgnoreCase(propName)) {
                    result.setRating(propValue);
                } else if ("ya:ovs:price".equalsIgnoreCase(propName)) {
                    result.setPrice(propValue);
                }
            }
        }
        Elements descriptions = doc.getElementsByClass("videoView-description");
        if (!descriptions.isEmpty()) {
            Element description = descriptions.get(0);
            Elements yearList = description.getElementsByClass("video-year");
            if (!yearList.isEmpty()) {
                Element year = yearList.get(0);
                result.setYear(year.text());
            }
        }
        Elements origTitles = doc.getElementsByClass("video-title-original");
        if (!origTitles.isEmpty()) {
            Element origTitle = origTitles.get(0);
            result.setTitleOriginal(origTitle.text());
        }

        Elements personHolders = doc.getElementsByClass("video-person");
        if (!personHolders.isEmpty()) {
            List<String> actors = new ArrayList<>();
            for (int i=0; i<personHolders.size(); i++) {
                Element personHolder = personHolders.get(i);
                Elements links = personHolder.getElementsByTag("a");
                if (!links.isEmpty()) {
                    Element link = links.get(0);
                    String role = link.attr("itemprop");
                    Elements nameHolders = link.getElementsByClass("video-person-name");
                    if (!nameHolders.isEmpty()) {
                        Element nameHolder = nameHolders.get(0);
                        String name = nameHolder.text();
                        if (name!=null) {
                            if ("actor".equalsIgnoreCase(role)) {
                                actors.add(name);
                            }
                            if ("director".equalsIgnoreCase(role)) {
                                result.setDirector(name);
                            }
                        }
                    }

                }
            }
            if (!actors.isEmpty()) {
                result.setActors(String.join(", ", actors));
            }
        }

        return result;
    }
}
