package com.nuix.relativityclient.utils;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import java.util.HashSet;
import java.util.Set;

public abstract class ApplicationLogger {

    private static final Logger LOGGER = LogManager.getLogger(ApplicationLogger.class.getName());
    private Set<StateListener> stateListeners = new HashSet<>();
    private String lastLogOperationName;
    private long lastLogMs;
    private static final long LOG_INTERVAL_MS=60000;

    protected Set<String> errorMessages = new HashSet<>();

    public abstract void logException(String s, Exception e);

    public abstract void logInfo(String s);

    public abstract void notifyComplete();

    public abstract void setStatus(String status, long currentValue, long estimatedFinalValue);

    public synchronized void addStateListener(StateListener stateListener) {
        stateListeners.add(stateListener);
    }

    public void notifyClosed(){
        for(StateListener stateListener : stateListeners){
            stateListener.stateClosed();
        }
    }

    public void logUniqueError(String s) {
        logUniqueError(s,null);
    }

    public void logUniqueError(String s, Exception e) {
        boolean messageUnique = errorMessages.add(s);
        if (messageUnique) {
            logException(s, e);
        } else {
            LOGGER.error(s, e);
        }
    }

    public void logError(String s) {
        logException(s,null);
    }

    public void logProgress(String operationName, long currentOperationId, long totalOperationsCount, long currentItemId, long totalItemsCount){
        boolean outputLog=false;
        if (lastLogOperationName ==null || !lastLogOperationName.equals(operationName)){
            outputLog=true;
        }

        if (currentItemId==0 || currentItemId==totalItemsCount-1){
            outputLog=true;
        }

        if (DateTime.now().getMillis()-lastLogMs>LOG_INTERVAL_MS){
            outputLog=true;
        }

        String statusText = operationName+"... "+(currentItemId+1)+" / "+totalItemsCount;

        long estimatedFinalValue = totalItemsCount*totalOperationsCount;
        long currentValue = totalItemsCount*currentOperationId + (currentItemId+1);
        setStatus(statusText,currentValue,estimatedFinalValue);

        if (outputLog){
            logInfo(DateTime.now().toString()+"\t"+statusText);
            lastLogOperationName =operationName;
            lastLogMs=DateTime.now().getMillis();
        }
    }

}
