package com.lumination.leadmelabs.services.jobServices;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentSender;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.lumination.leadmelabs.MainActivity;
import com.lumination.leadmelabs.utilities.Constants;

/**
 * Once a every set time period check if there is an update available on the play store, if there
 * is force an update.
 */
public class UpdateJobService extends JobService {
    private static final String TAG = "UpdateJobService";
    private static final int JOB_ID = 3;

    /**
     * Schedule a job to be performed at a periodic time. The timing is inexact so can happen a few
     * minutes either side of the require interval.
     * NOTE: the minimal time interval for scheduling is 15 minutes.
     */
    public static void schedule(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        ComponentName componentName = new ComponentName(context, UpdateJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, componentName);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        builder.setPeriodic(Constants.ONE_DAY_INTERVAL);
        builder.setRequiresDeviceIdle(true); //Only refresh if a user is not interacting with it
        int resultCode = jobScheduler.schedule(builder.build());

        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Job scheduled");
            checkForUpdate(); //Check on start up as well
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
        Log.d(TAG, "Performing Update Job");

        /* executing a task synchronously */
        //Check if signed in to play store??
        checkForUpdate();

        /* condition for finishing it */
        if (false) { //no finish condition as of yet
            // To finish a periodic JobService,
            // you must cancel it, so it will not be scheduled more.
            UpdateJobService.cancel(this);
        }

        // false when it is synchronous.
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    private static void checkForUpdate() {
        Log.i("Update", "Checking for update");
        // Returns an intent object that you use to check for an update.
        Task<AppUpdateInfo> appUpdateInfoTask = MainActivity.appUpdateManager.getAppUpdateInfo();

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                // Request the update.
                try {
                    MainActivity.appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            MainActivity.getInstance(),
                            Constants.UPDATE_REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }

            Log.i("Update Availability", String.valueOf(appUpdateInfo.updateAvailability()));
            Log.i("Update Priority", String.valueOf(appUpdateInfo.updatePriority()));
        });

        appUpdateInfoTask.addOnFailureListener(appUpdateInfo ->
                Log.i("Update", "No update available: " + appUpdateInfo.toString())
        );
    }
}
