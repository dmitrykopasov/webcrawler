package ru.croc.mts.crawler.worker;

import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import ru.croc.mts.crawler.Application;
import ru.croc.mts.crawler.task.Task;

import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.json.JSONObject;
import ru.croc.mts.crawler.util.ParserUtil;

public class Crawler implements Runnable {

    private Logger logger = Logger.getLogger(Crawler.class.getName());

    private static final int INDEX_LIMIT = 1;

    private static final String INDEX_URL_PATTERN = "https://megogo.ru/ru/films/main/?pageToken=PjI[index]&ajax=true&origin=%2Fru%2Ffilms%2Fmain&widget=widget_11";
    private static final String INDEX_PLACEHOLDER = "[index]";
    private static final String PAGE_URL_PATTERN = "https://megogo.ru[path]?video_view_tab=cast";
    private static final String PATH_PLACEHOLDER = "[path]";

    public void run(){
        while (Application.getCrawlersActive()) {
            Task task = Application.getTaskQueue().poll();
            if (task == null) {
                logger.info("Thread parked");
                LockSupport.park();
            } else {

                if (task.getIndex()!=null) {
                    logger.info("Going to get index #" + task.getIndex());
                    processIndex(task.getIndex());
                } else {
                    logger.info("Going to get page " + task.getPageUrl());
                    processUrl(task.getPageUrl());
                }

            }
        }

    }

    private void processIndex(int index){
        String url = INDEX_URL_PATTERN.replace(INDEX_PLACEHOLDER, String.valueOf(index));
        try {
            Content content = httpGet(url);
            if (content.getType().getMimeType().equalsIgnoreCase("application/json")) {
                logger.info("Index json received: " + content.asString());
                JSONObject resp = new JSONObject(content.asString());
                JSONObject data = resp.getJSONObject("data");
                if (data!=null) {
                    JSONObject widgets = data.getJSONObject("widgets");
                    if (widgets!=null) {
                        JSONObject widget_11 = widgets.getJSONObject("widget_11");
                        if (widget_11!=null) {
                            String html = widget_11.getString("html");
                            if (index<INDEX_LIMIT) {
                                Application.getTaskQueue().add(new Task(index + 1));
                            }
                            ParserUtil util = new ParserUtil(html);
                            List<Task> pageTasks = util.getAllUrls().stream().map(m -> new Task(m)).collect(Collectors.toList());
                            logger.info("Page tasks to add: " + pageTasks.size());
                            Application.getTaskQueue().addAll(pageTasks);
                        }
                    }
                }
            } else {
                logger.warning("Non-json response received");
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to get index #" + index, e);
        }
    }

    private void processUrl(String url){
        try {
            Content content = httpGet(PAGE_URL_PATTERN.replace(PATH_PLACEHOLDER, url));
            if (content.getType().getMimeType().equalsIgnoreCase("text/html")) {
                ParserUtil util = new ParserUtil(content.asString());
                Application.getResultQueue().add(util.parsePage());
            } else {
                logger.warning("Non-html page received");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to get page " + url, e);
        }
    }

    private Content httpGet(String url) throws Exception {
        if (Application.getUseProxy()) {
            List<String> proxies = Application.getProxyList();
            String proxy = proxies.get(new Random().nextInt(proxies.size()));
            return Request.Get(url).viaProxy(proxy).execute().returnContent();
        } else {
            return Request.Get(url).execute().returnContent();
        }
    }

}
