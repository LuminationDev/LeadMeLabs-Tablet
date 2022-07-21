package com.lumination.leadmelabs.services.jobServices;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import com.lumination.leadmelabs.services.NetworkService;
import com.lumination.leadmelabs.utilities.Constants;

/**
 * Once a every set time period refresh the connection with the NUC to receive the latest updates
 * and make sure the connection is present.
 */
public class RefreshJobService extends JobService {
    private static final String TAG = "RefreshJobService";
    private static final int JOB_ID = 2;

    /**
     * Schedule a job to be performed at a periodic time. The timing is inexact so can happen a few
     * minutes either side of the require interval.
     * NOTE: the minimal time interval for scheduling is 15 minutes.
     */
    public static void schedule(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        ComponentName componentName = new ComponentName(context, RefreshJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, componentName);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        builder.setPeriodic(Constants.THREE_HOUR_INTERVAL);
        builder.setRequiresDeviceIdle(true); //Only refresh if a user is not interacting with it
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

        /* executing a task synchronously */
        if(NetworkService.getNUCAddress() != null) {
            NetworkService.refreshNUCAddress();
        }

        /* condition for finishing it */
        if (false) { //no finish condition as of yet
            // To finish a periodic JobService,
            // you must cancel it, so it will not be scheduled more.
            RefreshJobService.cancel(this);
        }

        // false when it is synchronous.
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
