package com.kbizsoft.KPlayer.java;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.kbizsoft.KPlayer.gui.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadFileThread extends Thread {

    private static final String TAG = "DownloadFileThread";
    private final File outputFile;
    private final Context context;
    private final String videoUrl;
    private final Handler mainHandler;
    private final Handler handler;

    private final MainActivity mainActivity = new MainActivity();;


    public DownloadFileThread(String videoUrl, File outputFile, Context context, Handler handler) {
        this.outputFile = outputFile;
        this.context = context;
        this.videoUrl = videoUrl;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.handler = handler;
    }

    @Override
    public void run() {

        try {
            URL url = new URL(videoUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            int totalFileSize = connection.getContentLength();

            FileOutputStream outputStream = new FileOutputStream(outputFile);
            InputStream inputStream = connection.getInputStream();

            byte[] buffer = new byte[4096];
            int bytesRead;
            int totalBytesRead = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                final int progress = (int) ((totalBytesRead * 100) / totalFileSize);

                // Update progress on the main thread
                mainHandler.post(() -> updateProgress(progress));
            }

            outputStream.close();
            inputStream.close();
            connection.disconnect();

            mainHandler.post(() -> {
                showToast("Video downloaded successfully" + videoUrl);
                handler.sendEmptyMessage(0);
            });

            // handler.sendEmptyMessage(0);

        } catch (Exception e) {
            Log.e(TAG, "Error downloading video", e);
            mainHandler.post(() -> {
                showToast("Error downloading video: " + e.getMessage());
                outputFile.delete();
            });
        }
    }

    private void updateProgress(int progress) {
        // Update UI with the download progress (e.g., progress bar)
        // You can customize this method based on your UI requirements
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}

