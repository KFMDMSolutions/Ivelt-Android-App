package com.kfmdmsolutions.ivelt;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.kfmdmsolutions.ivelt.Utilities.Logger;


public class PowerNotificationUpdater extends Worker {
    public PowerNotificationUpdater(@NonNull Context context, @NonNull  WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Logger.getInstance(getApplicationContext()).log("NotifUpdate power work");
        NotificationService.checkForNotifications(getApplicationContext());
        return Result.success();
    }
}
