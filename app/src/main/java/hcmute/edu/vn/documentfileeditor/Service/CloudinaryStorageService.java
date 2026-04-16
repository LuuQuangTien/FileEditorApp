package hcmute.edu.vn.documentfileeditor.Service;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import hcmute.edu.vn.documentfileeditor.BuildConfig;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class CloudinaryStorageService {
    private static final MediaType MEDIA_TYPE_BINARY = MediaType.parse("application/octet-stream");

    private final OkHttpClient client;

    public CloudinaryStorageService() {
        this.client = new OkHttpClient();
    }

    public boolean isConfigured() {
        return !BuildConfig.CLOUDINARY_CLOUD_NAME.trim().isEmpty()
                && !BuildConfig.CLOUDINARY_UPLOAD_PRESET.trim().isEmpty();
    }

    public UploadResult uploadDocument(File file, String publicId, String folder) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("Cloudinary chua duoc cau hinh. Hay them CLOUDINARY_CLOUD_NAME va CLOUDINARY_UPLOAD_PRESET vao local.properties");
        }
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("Khong tim thay file de upload");
        }

        MultipartBody.Builder bodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", BuildConfig.CLOUDINARY_UPLOAD_PRESET.trim())
                .addFormDataPart(
                        "file",
                        file.getName(),
                        RequestBody.create(file, MEDIA_TYPE_BINARY)
                );

        if (publicId != null && !publicId.trim().isEmpty()) {
            bodyBuilder.addFormDataPart("public_id", publicId.trim());
        }
        if (folder != null && !folder.trim().isEmpty()) {
            bodyBuilder.addFormDataPart("folder", folder.trim());
        }

        Request request = new Request.Builder()
                .url(buildUploadUrl())
                .post(bodyBuilder.build())
                .build();

        try (Response response = client.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String rawBody = responseBody != null ? responseBody.string() : "";
            if (!response.isSuccessful()) {
                throw new IllegalStateException("Cloudinary upload that bai: HTTP " + response.code() + " - " + rawBody);
            }

            JSONObject json = new JSONObject(rawBody);
            String secureUrl = json.optString("secure_url");
            if (secureUrl.isEmpty()) {
                throw new IllegalStateException("Cloudinary khong tra ve secure_url hop le");
            }
            return new UploadResult(secureUrl, json.optString("public_id"));
        }
    }

    public void downloadToFile(String fileUrl, File targetFile) throws Exception {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Khong co URL de tai file");
        }
        if (targetFile == null) {
            throw new IllegalArgumentException("Khong co file dich de luu");
        }

        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        Request request = new Request.Builder().url(fileUrl).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("Tai file tu Cloudinary that bai: HTTP " + response.code());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IllegalStateException("Cloudinary tra ve du lieu rong");
            }

            try (InputStream inputStream = body.byteStream();
                 FileOutputStream outputStream = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        }
    }

    private String buildUploadUrl() {
        return "https://api.cloudinary.com/v1_1/" + BuildConfig.CLOUDINARY_CLOUD_NAME.trim() + "/raw/upload";
    }

    public static class UploadResult {
        private final String secureUrl;
        private final String publicId;

        public UploadResult(String secureUrl, String publicId) {
            this.secureUrl = secureUrl;
            this.publicId = publicId;
        }

        public String getSecureUrl() {
            return secureUrl;
        }

        public String getPublicId() {
            return publicId;
        }
    }
}
