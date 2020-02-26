package com.nuix.relativityclient.utils;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.concurrent.*;

//Used to prevent blocking of Swing EventDispatchThread while waiting for network (or any other time-consuming) requests
public class TaskDelegator {
    private static final Logger LOGGER = LogManager.getLogger(TaskDelegator.class.getName());

    private ExecutorService executor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    public void submit(Runnable task) {
        submit(task, false);
    }

    //@param: clearQueue, force flush queue when tasks become irrelevant to context
    public void submit(Runnable task, boolean clearQueue) {
        try {
            if (clearQueue) {
                ((ThreadPoolExecutor) executor).getQueue().clear();
            }

            executor.submit(task);

        } catch (RejectedExecutionException e) {
            LOGGER.error("Cannot submit task", e);
        }
    }

    //Requires each running task to have implemented a way to handle InterruptedExecution to exit
    public void forceShutdown() {
        executor.shutdownNow();
    }
}
