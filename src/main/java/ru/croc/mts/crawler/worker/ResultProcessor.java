package ru.croc.mts.crawler.worker;

import com.opencsv.CSVWriter;
import ru.croc.mts.crawler.Application;
import ru.croc.mts.crawler.task.Result;

import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResultProcessor implements Runnable {

    private Logger logger = Logger.getLogger(ResultProcessor.class.getName());

    public void run(){
        try {
            CSVWriter writer = new CSVWriter(new FileWriter("result.csv"));
            while (Application.getCrawlersActive()) {
                Result result = Application.getResultQueue().poll();
                if (result == null) {
                    Thread.yield();
                } else {
                    writer.writeNext(result.getAsArray());
                    writer.flush();
                }
            }
            writer.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to write down the results!", e);
        }
    }
}
