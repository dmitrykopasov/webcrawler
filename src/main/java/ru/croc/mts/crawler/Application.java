package ru.croc.mts.crawler;

import ru.croc.mts.crawler.task.Result;
import ru.croc.mts.crawler.task.Task;
import ru.croc.mts.crawler.worker.Crawler;
import ru.croc.mts.crawler.worker.ResultProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

public class Application {
    /**
     * Number of crawler threads
     */
    private static final int CRAWLER_NUM = 5;
    /**
     * Task queue limit, once exceeded - parked threads will be unparked
     */
    private static final int UNPARK_LIMIT = 5;

    private static ConcurrentLinkedQueue<Task> taskQueue = new ConcurrentLinkedQueue<Task>();
    private static ConcurrentLinkedQueue<Result> resultQueue = new ConcurrentLinkedQueue<Result>();

    private static Thread[] threads = new Thread[CRAWLER_NUM];
    private static Thread rsThread;

    private static volatile boolean crawlersActive = true;

    private static boolean useProxy = false;
    private static List<String> proxyList = null;


    public static void main(String[] args){
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Properties properties = new Properties();
        try (InputStream is = loader.getResourceAsStream("application.properties")) {
            properties.load(is);
            if ("proxy".equalsIgnoreCase(properties.getProperty("http.connection"))) {
                useProxy=true;
                proxyList=new ArrayList<>();
                int i=0;
                while (true) {
                    i++;
                    String value = properties.getProperty("http.proxy." + i);
                    if (value!=null) {
                        proxyList.add(value);
                    } else {
                        break;
                    }
                }
                if (proxyList.isEmpty()) {
                    throw new RuntimeException("Proxy list is empty!");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read application.properties");
        }

        taskQueue.add(new Task(0));

        for (int i=0; i<CRAWLER_NUM; i++) {
            Thread thread = new Thread(new Crawler());
            threads[i]=thread;
            thread.start();
        }

        ResultProcessor resultProcessor = new ResultProcessor();
        rsThread = new Thread(resultProcessor);
        rsThread.start();

        while (crawlersActive) {
            // If tasks queue exceeds a limit, let's unpark some of the parked thread, if any
            if (taskQueue.size()>UNPARK_LIMIT) {
                for (int i=0; i<CRAWLER_NUM; i++) {
                    if (threads[i].getState()==Thread.State.WAITING) {
                        LockSupport.unpark(threads[i]);
                    }
                }
            } else if (taskQueue.isEmpty()) {
                int totalParked = 0;
                for (int i=0; i<CRAWLER_NUM; i++) {
                    if (threads[i].getState()==Thread.State.WAITING) {
                        totalParked++;
                    }
                }
                // If task queue is empty and crawler threads have parked themselves - it means that we are done with crawling
                // In this case let's unpark crawler threads to let them finish
                if (totalParked==CRAWLER_NUM && taskQueue.isEmpty()) {
                    crawlersActive = false;
                    for (int i=0; i<CRAWLER_NUM; i++) {
                        LockSupport.unpark(threads[i]);
                    }
                }
            }
            Thread.yield();
        }

        try {
            // Only need to wait while result thread drops data to the disk
            rsThread.join();
        } catch (InterruptedException e) {

        }

    }

    public static ConcurrentLinkedQueue<Task> getTaskQueue(){
        return taskQueue;
    }

    public static ConcurrentLinkedQueue<Result> getResultQueue(){
        return resultQueue;
    }

    public static boolean getCrawlersActive(){
        return crawlersActive;
    }

    public static boolean getUseProxy(){
        return useProxy;
    }

    public static List<String> getProxyList(){
        return proxyList;
    }
}
