package com.android.cts.ivelt;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Utils {
    static final Executor executor = Executors.newSingleThreadExecutor();
    static void executeAsync(Runnable runnable){
        executor.execute(runnable);
    }
}
