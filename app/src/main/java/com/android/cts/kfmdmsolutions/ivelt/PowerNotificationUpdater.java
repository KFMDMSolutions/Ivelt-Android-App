package com.android.cts.kfmdmsolutions.ivelt;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.cts.kfmdmsolutions.ivelt.Utilities.Logger;


public class PowerNotificationUpdater extends Worker {
    public PowerNotificationUpdater(@NonNull Context context, @NonNull  WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @NonNull
    @Override
    public Result doWork() {
        Logger.getInstance(getApplicationContext()).log("NotifUpdate power work");
        NotificationService.checkForNotifications(getApplicationContext());
        return Result.success();
    }
}
