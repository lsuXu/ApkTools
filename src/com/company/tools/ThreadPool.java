package com.company.tools;

import java.util.concurrent.*;

public class ThreadPool extends ThreadPoolExecutor {

    private static final ThreadPool INSTANCE = new ThreadPool();

    private static final int MAX_CORE_POOL_SIZE = 3,MAX_POOL_SIZE = 8,KEEP_ALIVE_TIME = 10;

    private ThreadPool(){
        super(MAX_CORE_POOL_SIZE,MAX_POOL_SIZE,KEEP_ALIVE_TIME,TimeUnit.SECONDS,new LinkedBlockingDeque<>(),new PoolThreadFactory());
    }

    public static ThreadPool getInstance(){
        return INSTANCE;
    }

    private final static class PoolThreadFactory implements ThreadFactory{

        int threadIndex = 0;

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName(String.format("CommonThread-%s",++threadIndex));
            return thread;
        }
    }

}
