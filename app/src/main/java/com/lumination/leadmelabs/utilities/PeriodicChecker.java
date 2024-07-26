package com.lumination.leadmelabs.utilities;

import android.os.Handler;

public class PeriodicChecker {
    private final Handler handler = new Handler();
    private final Runnable periodicRunnable = new Runnable() {
        @Override
        public void run() {
            if (callback != null && callback.onPeriodicCheck()) {
                stop(); // Stop the periodic check if callback returns true
                return;
            }

            // 1 second by default
            handler.postDelayed(this, 1000);
        }
    };
    private PeriodicCheckCallback callback;

    public void start() {
        handler.post(periodicRunnable);
    }

    public void stop() {
        handler.removeCallbacks(periodicRunnable);
    }

    public void setCallback(PeriodicCheckCallback callback) {
        this.callback = callback;
    }

    public interface PeriodicCheckCallback {
        boolean onPeriodicCheck();
    }
}
