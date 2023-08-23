package com.lumination.leadmelabs.utilities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.widget.ImageView;

/**
 * Responsible for the starting or stopping of 'flashing' an icon (imageView) at a set duration.
 */
public class IconAnimator {
    private final ImageView imageView;
    private final ObjectAnimator animator;
    private boolean isFlashing;
    private final int duration = 1000;

    public IconAnimator(ImageView imageView) {
        this.imageView = imageView;

        //0.2f as a minimum so that it doesn't fully disappear
        animator = ObjectAnimator.ofFloat(imageView, "alpha", 1f, 0.2f);
        animator.setDuration(duration);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.REVERSE);

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isFlashing) {
                    animator.start();
                }
            }
        });
    }

    public ImageView getImageView() {
        return this.imageView;
    }

    public void startFlashing() {
        isFlashing = true;
        animator.start();
    }

    public void stopFlashing() {
        isFlashing = false;
        animator.cancel();

        // Calculate scaled duration based on the current alpha value
        float currentAlpha = imageView.getAlpha();
        long scaledDuration = (long) (duration * (1.0f - currentAlpha));

        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(imageView, "alpha", currentAlpha, 1f);
        alphaAnimator.setDuration(scaledDuration);
        alphaAnimator.start();
    }
}
