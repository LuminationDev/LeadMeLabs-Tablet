package com.lumination.leadmelabs;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestUtils {
    //Helper function to test LiveData attributes
    public static <T> T getOrAwaitValue(final LiveData<T> liveData) throws InterruptedException {
        final Object[] data = new Object[1];
        final CountDownLatch latch = new CountDownLatch(1);
        Observer<T> observer = new Observer<T>() {
            @Override
            public void onChanged(@Nullable T o) {
                data[0] = o;
                latch.countDown();
                liveData.removeObserver(this);
            }
        };
        liveData.observeForever(observer);
        // Don't wait indefinitely if the LiveData is not set.
        if (!latch.await(2, TimeUnit.SECONDS)) {
            throw new RuntimeException("LiveData value was never set.");
        }
        //noinspection unchecked
        return (T) data[0];
    }

    //Perform an actual HTTP request to test if links are valid and extract the response code
    public static int MimicHttpRequest(String URL) {
        HttpClient httpclient = HttpClientBuilder.create().build();
        StatusLine statusLine = null;
        
        try {
            HttpResponse response = httpclient.execute(new HttpGet(URL));
            statusLine = response.getStatusLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert statusLine != null;
        return statusLine.getStatusCode();
    }
}
