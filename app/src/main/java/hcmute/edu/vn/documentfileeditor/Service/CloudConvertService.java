package hcmute.edu.vn.documentfileeditor.Service;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import hcmute.edu.vn.documentfileeditor.BuildConfig;

public class CloudConvertService {
    private static final String TAG = "CloudConvertService";

    private static final String API_KEY = BuildConfig.CLOUDCONVERT_API_KEY;

    private final OkHttpClient client;
    private final ExecutorService executor;

    public CloudConvertService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public interface CloudConvertCallback {
        void onProgress(String message);

        void onSuccess(String pdfLocalPath);

        void onFailure(String error);
    }

    public void convertDocument(Context context, Uri docUri, String outputFileName, String outputFormat,
            CloudConvertCallback callback) {
        executor.execute(() -> {
            try {
                if (API_KEY == null || API_KEY.isEmpty() || API_KEY.equals("AAAAAAAA")) {
                    postMain(context, callback, false,
                            "Lỗi: API Key chưa chính xác hoặc không có", null);
                    return;
                }

                File tempDocFile = copyUriToTempFile(context, docUri, "temp_doc_upload");
                if (tempDocFile == null) {
                    postMain(context, callback, false, "Không thể đọc file đã chọn.", null);
                    return;
                }

                postMain(context, callback, true, "1/4. Khởi tạo tác vụ trên CloudConvert...", null);
                JSONObject jobData = createJob(outputFormat);
                if (jobData == null) {
                    postMain(context, callback, false, "Lỗi khi kết nối CloudConvert Server.", null);
                    return;
                }

                String uploadUrl = extractUploadUrl(jobData);
                JSONObject uploadParams = extractUploadParams(jobData);
                String jobId = jobData.optString("id");

                if (uploadUrl == null || uploadParams == null || jobId.isEmpty()) {
                    postMain(context, callback, false, "Lỗi phân tích Job Request.", null);
                    return;
                }

                postMain(context, callback, true, "2/4. Đang tải file lên đám mây...", null);
                boolean uploaded = uploadFile(uploadUrl, uploadParams, tempDocFile);
                if (!uploaded) {
                    postMain(context, callback, false, "Lỗi khi tải file lên mây.", null);
                    return;
                }

                postMain(context, callback, true, "3/4. Đang xử lý chuyển đổi...", null);
                String exportUrl = pollJobUntilFinished(jobId);
                if (exportUrl == null) {
                    postMain(context, callback, false, "Quá trình convert bị lỗi hoặc hết hạn ngạch (Quota Exceeded).",
                            null);
                    return;
                }

                postMain(context, callback, true, "4/4. Đang tải file về máy...", null);
                File pdfDir = new File(context.getFilesDir(), "scanned_pdfs");
                if (!pdfDir.exists()) {
                    pdfDir.mkdirs();
                }
                String safeName = outputFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
                File resultFile = new File(pdfDir, safeName + "." + outputFormat);

                boolean downloaded = downloadFile(exportUrl, resultFile);
                if (!downloaded) {
                    postMain(context, callback, false, "Lỗi khi tải file về.", null);
                    return;
                }

                tempDocFile.delete(); // Cleanup

                // Done!
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    callback.onSuccess(resultFile.getAbsolutePath());
                });

            } catch (Exception e) {
                Log.e(TAG, "CloudConvert loop failed", e);
                String msg = e.getMessage();
                if (msg != null && msg.contains("402")) {
                    postMain(context, callback, false,
                            "Lỗi 402: Bạn đã hết Quota (lượt chuyển đổi) trong ngày hôm nay.", null);
                } else if (msg != null && msg.contains("429")) {
                    postMain(context, callback, false, "Lỗi 429: Gửi request quá nhanh. Vui lòng thử lại sau.", null);
                } else {
                    postMain(context, callback, false, "Lỗi hệ thống: " + msg, null);
                }
            }
        });
    }

    // Keep the old method for backwards compatibility
    public void convertDocumentToPdf(Context context, Uri docUri, String outputFileName,
            CloudConvertCallback callback) {
        convertDocument(context, docUri, outputFileName, "pdf", callback);
    }

    private void postMain(Context context, CloudConvertCallback callback, boolean isProgress, String msg, String path) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            if (isProgress && msg != null) {
                callback.onProgress(msg);
            } else if (!isProgress && msg != null) {
                callback.onFailure(msg);
            }
        });
    }

    private JSONObject createJob(String outputFormat) throws Exception {
        String jsonPayload = "{" +
                "\"tasks\": {" +
                "\"import-1\": {\"operation\": \"import/upload\"}," +
                "\"task-1\": {\"operation\": \"convert\",\"input\": \"import-1\",\"output_format\": \"" + outputFormat + "\"}," +
                "\"export-1\": {\"operation\": \"export/url\",\"input\": \"task-1\"}" +
                "}}";

        RequestBody body = RequestBody.create(jsonPayload, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url("https://api.cloudconvert.com/v2/jobs")
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Create Job failed: " + response.code());
            }
            if (response.body() == null)
                return null;
            return new JSONObject(response.body().string()).getJSONObject("data");
        }
    }

    private boolean uploadFile(String url, JSONObject params, File file) throws Exception {
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        Iterator<String> keys = params.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            builder.addFormDataPart(key, params.getString(key));
        }

        RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/octet-stream"));
        builder.addFormDataPart("file", file.getName(), fileBody);

        Request request = new Request.Builder()
                .url(url)
                .post(builder.build())
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful() || response.code() == 303;
        }
    }

    private String pollJobUntilFinished(String jobId) throws Exception {
        Request request = new Request.Builder()
                .url("https://api.cloudconvert.com/v2/jobs/" + jobId)
                .get()
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        while (true) {
            Thread.sleep(2000); // 2 second delay between queries
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Poll failed: " + response.code());
                }
                if (response.body() == null)
                    continue;
                JSONObject data = new JSONObject(response.body().string()).getJSONObject("data");
                String status = data.optString("status");
                if ("finished".equals(status)) {
                    return extractExportUrl(data);
                } else if ("error".equals(status)) {
                    throw new RuntimeException("Job failed remotely.");
                }
            }
        }
    }

    private boolean downloadFile(String url, File target) throws Exception {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful())
                return false;
            ResponseBody body = response.body();
            if (body == null)
                return false;

            try (InputStream is = body.byteStream();
                    FileOutputStream fos = new FileOutputStream(target)) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            }
            return true;
        }
    }

    private String extractUploadUrl(JSONObject jobData) throws Exception {
        JSONArray tasks = jobData.optJSONArray("tasks");
        if (tasks == null)
            return null;
        for (int i = 0; i < tasks.length(); i++) {
            JSONObject task = tasks.getJSONObject(i);
            if ("import/upload".equals(task.optString("operation"))) {
                return task.getJSONObject("result").getJSONObject("form").getString("url");
            }
        }
        return null;
    }

    private JSONObject extractUploadParams(JSONObject jobData) throws Exception {
        JSONArray tasks = jobData.optJSONArray("tasks");
        if (tasks == null)
            return null;
        for (int i = 0; i < tasks.length(); i++) {
            JSONObject task = tasks.getJSONObject(i);
            if ("import/upload".equals(task.optString("operation"))) {
                return task.getJSONObject("result").getJSONObject("form").getJSONObject("parameters");
            }
        }
        return null;
    }

    private String extractExportUrl(JSONObject jobData) throws Exception {
        JSONArray tasks = jobData.optJSONArray("tasks");
        if (tasks == null)
            return null;
        for (int i = 0; i < tasks.length(); i++) {
            JSONObject task = tasks.getJSONObject(i);
            if ("export/url".equals(task.optString("operation"))) {
                JSONArray files = task.getJSONObject("result").getJSONArray("files");
                if (files.length() > 0) {
                    return files.getJSONObject(0).getString("url");
                }
            }
        }
        return null;
    }

    private File copyUriToTempFile(Context context, Uri uri, String prefix) {
        try {
            File tempFile = File.createTempFile(prefix, ".tmp", context.getCacheDir());
            try (InputStream is = context.getContentResolver().openInputStream(uri);
                    FileOutputStream fos = new FileOutputStream(tempFile)) {
                if (is == null)
                    return null;
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                return tempFile;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
