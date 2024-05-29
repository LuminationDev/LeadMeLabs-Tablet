package com.lumination.leadmelabs.services.jobServices;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import com.lumination.leadmelabs.managers.FirebaseManager;
import com.lumination.leadmelabs.utilities.Constants;

/**
 * Once a every set time period use the FirebaseManager to query if the application has a valid
 * license key. The service is registered in the Manifest and is assigned to stop with the task.
 */
public class NotificationJobService extends JobService {
    private static final String TAG = "LicenseJobService";
    private static final int JOB_ID = 2;
    private static final long ONE_HOUR_MILLIS = 3600000L;
    public static long lastRunTimestamp = 0;

    /**
     * Schedule a job to be performed at a periodic time. The timing is inexact so can happen a few
     * minutes either side of the require interval.
     * NOTE: the minimal time interval for scheduling is 15 minutes.
     */
    public static void schedule(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        ComponentName componentName = new ComponentName(context, NotificationJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, componentName);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        builder.setPeriodic(Constants.MINIMAL_INTERVAL);
        int resultCode = jobScheduler.schedule(builder.build());

        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Job scheduled");
        } else {
            Log.d(TAG, "Job scheduling failed");
        }
    }

    public static void cancel(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(JOB_ID);
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        Log.d(TAG, "Performing Job");

        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - lastRunTimestamp >= ONE_HOUR_MILLIS) {
            /* executing a task synchronously */
            FirebaseManager.checkForNotifications();
            lastRunTimestamp = 0;
        } else {
            Log.e(TAG, "Job has been snoozed");
        }

        /* condition for finishing it */
        if (false) { //no finish condition as of yet
            // To finish a periodic JobService,
            // you must cancel it, so it will not be scheduled more.
            NotificationJobService.cancel(this);
        }

        // false when it is synchronous.
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
