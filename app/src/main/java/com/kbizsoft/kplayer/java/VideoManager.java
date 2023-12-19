package com.kbizsoft.KPlayer.java;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.kbizsoft.KPlayer.R;
import com.kbizsoft.KPlayer.gui.MainActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class VideoManager {

    private static final String BASE_URL = "https://kbizsoft.com/do_not_delete/"; // Replace with your server base URL
    private static final String TAG = VideoManager.class.getSimpleName();
    private static final String CHANNEL_ID = "video_download_channel";
    private static final int NOTIFICATION_ID = 908;

    public static boolean is_running = false;
    public static Handler handler;

    private static final ArrayList<String> ALLOWED_FILENAMES = new ArrayList<String>() {};
    private VideoService videoService;

    public VideoManager() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        videoService = retrofit.create(VideoService.class);
    }

    public void downloadVideoInBackground(String videoUrl, Context context, String fileName) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            createNotificationChannel(context);
//        }

        // Use AsyncTask for background execution
        // Toast.makeText(context, "AsyncTask for background execution", Toast.LENGTH_LONG).show();
        // new DownloadVideoTask(videoUrl, context, fileName).execute();

        DownloadVideoTaskThread(videoUrl, context, fileName);
    }


    public void syncVideos(String timestamp, Context context) {
        // Make a Retrofit call to fetch video data from the server with the specified time parameter

        Call<ResponseBody> call = videoService.getVideoData(timestamp);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if (response.isSuccessful() && response.body() != null) {
                    getVideoFileDetails(response.body(), context);
                } else {
                    Log.e( TAG, "Failed to fetch video data");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e( TAG, "Network error: " + t.getMessage());
            }
        });


    }

    private void getVideoFileDetails(ResponseBody body, Context context) {

        // Handle the response and initiate the download process
        String jsonData = parseResponseBody(body);
        List<VideoInfo> videoList = parseJsonData(jsonData);

        // Empty existing videos list
        ALLOWED_FILENAMES.clear();

        // Iterate through the list and do whatever you need with the videos
        for (VideoInfo videoInfo : videoList) {

            // Add file name for match into folder
            ALLOWED_FILENAMES.add(videoInfo.getFileName());

            downloadVideoInBackground(videoInfo.getUrl(), context, videoInfo.getFileName());
        }

        checkAndDeleteFiles(context);
    }

    public boolean isFileExists(String filePath) {
        Log.e( TAG, "isFileExists, File Name: " + filePath);
        File file = new File(filePath);
        return file.exists();
    }

    private String parseResponseBody(ResponseBody body) {
        try {
            return body.string();
        } catch (IOException e) {
            Log.e( TAG, "Error reading response body: " + e.getMessage());
            return "";
        }
    }

    private List<VideoInfo> parseJsonData(String jsonData) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<VideoInfo>>(){}.getType();
        return gson.fromJson(jsonData, listType);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Video Download Channel";
            String description = "Channel for video download notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            notificationChannel.setDescription(description);

            notificationChannel.setSound(null, null);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    private void showNotification(Context context, String title, String content, int progress, boolean onGoing) {

        if(NotificationManagerCompat.from(context).areNotificationsEnabled()){

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setSmallIcon(R.drawable.icon)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.icon))
                    .setProgress(100, progress, false)
                    .setAutoCancel(true);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }


    public void DownloadVideoTaskThread (String videoUrl, Context context, String fileName){
        try {
            if (isExternalStorageWritable()) {
                saveToExternalStorage(videoUrl, fileName, context);
            } else {
                saveToMediaDirectory(videoUrl, fileName, context);
            }
        } catch (IOException e) {
            Toast.makeText(context, "Error:" + e.getMessage(), Toast.LENGTH_SHORT).show();
            throw new RuntimeException(e);
        }
    }

    private boolean isExternalStorageWritable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(Environment.isExternalStorageManager()){
                String state = Environment.getExternalStorageState();
                return Environment.MEDIA_MOUNTED.equals(state);
            }
        }
        return false;
    }

    private void saveToExternalStorage(String videoUrl, String fileName, Context context) throws IOException {
        File externalDir = Environment.getExternalStorageDirectory();
        File kplayerDir = new File(externalDir, "kplayer");

        if (!kplayerDir.exists()) {
            if (!kplayerDir.mkdirs()) {
                Log.e( TAG, "MKN BG: Failed to create directory");
                Toast.makeText(context, "Error: Failed to create directory", Toast.LENGTH_SHORT).show();
            }
        }

        File file = new File(kplayerDir, fileName);
        downloadAndSaveFile(videoUrl, file, context, kplayerDir);
    }

    private void saveToMediaDirectory(String videoUrl, String fileName, Context context) throws IOException {
        File moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        File file = new File(moviesDir, fileName);
        downloadAndSaveFile(videoUrl, file, context, moviesDir);
    }

    private void downloadAndSaveFile(String videoUrl, File outputFile, Context context, File kplayerDir) {

        String fileName = outputFile.getName();
        String filePath = kplayerDir.getPath() + "/" + fileName;

        if(isFileExists(filePath)){
            //Toast.makeText(context, "File Already Exist: " + fileName + "\nGoing to Cancel it", Toast.LENGTH_SHORT).show();
            return;
        }

        DownloadFileThread downloadThread = new DownloadFileThread(videoUrl, outputFile, context, handler);
        downloadThread.start();

    }

    public void checkAndDeleteFiles(Context context) {

        File kplayerDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);

        if (isExternalStorageWritable()) {
            File externalDir = Environment.getExternalStorageDirectory();
            kplayerDir = new File(externalDir, "kplayer");
        }

        String DIRECTORY_PATH = kplayerDir.getPath();

        File directory = new File(DIRECTORY_PATH);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();

            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();

                    if (!ALLOWED_FILENAMES.contains(fileName)) {
                        // File is not in the allowed list, so delete it
                        file.delete();
                    }
                }
            }
        }

        // Toast.makeText(context, "Sending Message for refresh", Toast.LENGTH_SHORT).show();
        handler.sendEmptyMessage(0);

    }





    /*
    private class DownloadVideoTask extends AsyncTask<Void, Integer, Boolean> {

        private final String videoUrl;
        private final Context context;
        private final String fileName;

        DownloadVideoTask(String videoUrl, Context context, String fileName) {
            this.videoUrl = videoUrl;
            this.context = context;
            this.fileName = fileName;
        }

        private void showNotification(Context context, String title, String content, int progress, boolean onGoing) {

            if(NotificationManagerCompat.from(context).areNotificationsEnabled()){

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setSmallIcon(R.drawable.icon)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.icon))
                        .setProgress(100, progress, false)
                        .setAutoCancel(true);

                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(NOTIFICATION_ID, builder.build());
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // showNotification(context, "Downloading "+fileName+" Video", "Download in progress...", 0, true);
            // Log.e( TAG, "Downloading "+fileName+" Video");
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                Log.e( TAG, "BG Task: Downloading Video into backgroud");
                // Check if external storage (SD card) is available
                if (isExternalStorageWritable()) {
                     return saveToExternalStorage(videoUrl, fileName);
                } else {
                    return saveToMediaDirectory(videoUrl, fileName);
                }

            } catch (Exception e) {
                // Toast.makeText(context, "BG Task: Error saving video: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e( TAG, "BG Task: Error saving video: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            showNotification(context, "Downloading "+fileName+" Video", "Download in progress...", values[0], true);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (result) {
                Log.e( TAG, "BG Task: Video download complete");
                showNotification(context, "Download "+fileName+" Complete", "Video download complete", 100, false);
            } else {
                Log.e( TAG, "BG Task: Video download failed");
                showNotification(context, "Download "+fileName+" Failed", "Video download failed", 0, false);
            }
        }

        private boolean isExternalStorageWritable() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if(Environment.isExternalStorageManager()){
                    String state = Environment.getExternalStorageState();
                    return Environment.MEDIA_MOUNTED.equals(state);
                }
            }
            return false;
        }

        private boolean saveToExternalStorage(String videoUrl, String fileName) throws IOException {
            File externalDir = Environment.getExternalStorageDirectory();
            File kplayerDir = new File(externalDir, "kplayer");

            if (!kplayerDir.exists()) {
                if (!kplayerDir.mkdirs()) {
                    Log.e( TAG, "MKN BG: Failed to create directory");
                    return false;
                }
            }

            File file = new File(kplayerDir, fileName);
            return downloadAndSaveFile(videoUrl, file, kplayerDir);
        }

        private boolean saveToMediaDirectory(String videoUrl, String fileName) throws IOException {
            File moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
            File file = new File(moviesDir, fileName);
            return downloadAndSaveFile(videoUrl, file, moviesDir);
        }

        private boolean downloadAndSaveFile(String videoUrl, File outputFile, File kplayerDir) {

            String fileName = outputFile.getName();
            String filePath = kplayerDir.getPath() + "/" + fileName;

            //Toast.makeText(context, "Downloading filePath: " + filePath, Toast.LENGTH_SHORT).show();
            Log.e( TAG, "Downloading filePath: " + filePath);

            try {
                URL url = new URL(videoUrl);
                // Log.e( TAG, "url: "+url.getPath());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                // Get the total file size for tracking progress
                int totalFileSize = connection.getContentLength();

                // Create a FileOutputStream for the file
                FileOutputStream outputStream = new FileOutputStream(filePath);

                // Create an InputStream to read the data from the connection
                InputStream inputStream = connection.getInputStream();

                byte[] buffer = new byte[4096];
                int bytesRead;
                int totalBytesRead = 0;

                int progress = 0;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    // You can use publishProgress() to update progress if needed

                    // Calculate progress percentage
                     int progressNew = (int) ((totalBytesRead * 100) / totalFileSize);

                     if(progress != progressNew) {
                         // Update progress in notification
                         progress = progressNew;
                         publishProgress(progress);
                     }

                }

                outputStream.close();
                inputStream.close();
                connection.disconnect();

                return true;
            } catch (IOException e) {
                Log.e(TAG , "Error: Error downloading video", e);
                return false;
            }
        }
    }


     */
}

