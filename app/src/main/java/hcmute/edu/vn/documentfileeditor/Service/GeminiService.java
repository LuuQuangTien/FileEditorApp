package hcmute.edu.vn.documentfileeditor.Service;

import android.util.Log;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class GeminiService {
    private static final String TAG = "GeminiService";

    // Danh sách API keys từ BuildConfig (phân cách bằng dấu phẩy trong local.properties)
    private static final String[] API_KEYS;
    static {
        String raw = hcmute.edu.vn.documentfileeditor.BuildConfig.GEMINI_API_KEYS;
        if (raw != null && !raw.isEmpty()) {
            API_KEYS = raw.split(",");
        } else {
            API_KEYS = new String[]{};
        }
    }

    // Chỉ số key hiện tại (thread-safe), xoay vòng giữa các key
    private static final AtomicInteger currentKeyIndex = new AtomicInteger(0);

    // Base URL cho Gemini API
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    // Model chính: gemini-2.0-flash - free tier cao nhất (15 RPM, 1500 RPD)
    private static final String PRIMARY_MODEL = "gemini-2.0-flash";

    // Model dự phòng: gemini-2.0-flash-lite - nhẹ hơn, quota riêng
    private static final String FALLBACK_MODEL = "gemini-2.0-flash-lite";

    // Số lần retry tối đa
    private static final int MAX_RETRIES = 3;

    private final OkHttpClient client;

    public interface GeminiCallback {
        void onSuccess(String result);
        void onTokenUsage(int totalTokenCount);
        void onError(String error);
    }

    public GeminiService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Lấy API key tiếp theo trong danh sách (xoay vòng)
     */
    private String getNextApiKey() {
        if (API_KEYS.length == 0) return "";
        int index = currentKeyIndex.getAndUpdate(i -> (i + 1) % API_KEYS.length);
        return API_KEYS[index].trim();
    }

    /**
     * Xử lý text bằng AI
     */
    public void processText(String prompt, String content, GeminiCallback callback) {
        if (API_KEYS.length == 0) {
            callback.onError("Chưa cấu hình API key. Vui lòng thêm GEMINI_API_KEYS trong local.properties");
            return;
        }
        executeWithRetry(prompt, content, PRIMARY_MODEL, getNextApiKey(), 0, 0, callback);
    }

    /**
     * Thực hiện request với retry + xoay key + fallback model
     * @param keysTriedCount: số key đã thử (để tránh vòng lặp vô hạn)
     */
    private void executeWithRetry(String prompt, String content, String model, String apiKey,
                                  int attempt, int keysTriedCount, GeminiCallback callback) {
        try {
            // Tạo JSON request body
            JSONObject jsonRequest = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject userContent = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject textPart = new JSONObject();

            textPart.put("text", prompt + ":\n\n" + content);
            parts.put(textPart);
            userContent.put("parts", parts);
            contents.put(userContent);
            jsonRequest.put("contents", contents);

            RequestBody body = RequestBody.create(
                    jsonRequest.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            String url = BASE_URL + model + ":generateContent?key=" + apiKey;

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Log.d(TAG, "Requesting [model=" + model + ", key=..." + apiKey.substring(Math.max(0, apiKey.length() - 4))
                    + ", attempt=" + (attempt + 1) + ", keysTried=" + (keysTriedCount + 1) + "]");

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Network error: " + e.getMessage());
                    if (attempt < MAX_RETRIES - 1) {
                        retryWithDelay(prompt, content, model, apiKey, attempt + 1, keysTriedCount, callback);
                    } else {
                        callback.onError("Lỗi mạng: " + e.getMessage());
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Response " + response.code() + " [model=" + model + "]");

                    if (response.isSuccessful()) {
                        handleSuccessResponse(responseBody, callback);
                    } else {
                        handleErrorResponse(response.code(), responseBody, prompt, content,
                                model, apiKey, attempt, keysTriedCount, callback);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Request build failed: " + e.getMessage());
            callback.onError("Không thể tạo request.");
        }
    }

    private void handleSuccessResponse(String responseBody, GeminiCallback callback) {
        try {
            JSONObject jsonResponse = new JSONObject(responseBody);
            String result = jsonResponse.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");
            
            int totalTokens = 0;
            if (jsonResponse.has("usageMetadata")) {
                JSONObject usage = jsonResponse.getJSONObject("usageMetadata");
                totalTokens = usage.optInt("totalTokenCount", 0);
            }
            
            int finalTotalTokens = totalTokens;
            callback.onTokenUsage(finalTotalTokens);
            callback.onSuccess(result.trim());
        } catch (Exception e) {
            Log.e(TAG, "Parse error: " + e.getMessage());
            callback.onError("Lỗi phân tích phản hồi từ AI.");
        }
    }

    /**
     * Xử lý lỗi thông minh:
     * 429/503 → thử key khác → thử fallback model → retry với delay
     */
    private void handleErrorResponse(int code, String responseBody, String prompt, String content,
                                     String model, String currentKey, int attempt, int keysTriedCount,
                                     GeminiCallback callback) {
        Log.w(TAG, "Error " + code + ": " + responseBody);

        // Lỗi 429 (Quota/Rate Limit) hoặc 503 (High Demand)
        if (code == 429 || code == 503) {

            // Chiến lược 1: Thử key khác (nếu còn key chưa thử)
            if (keysTriedCount < API_KEYS.length - 1) {
                String nextKey = getNextApiKey();
                Log.d(TAG, "Switching to next API key...");
                retryWithDelay(prompt, content, model, nextKey, 0, keysTriedCount + 1, callback);
                return;
            }

            // Chiến lược 2: Đã thử hết key với model chính → chuyển sang fallback model + reset keys
            if (model.equals(PRIMARY_MODEL)) {
                Log.d(TAG, "All keys exhausted for " + PRIMARY_MODEL + ", switching to " + FALLBACK_MODEL);
                String freshKey = getNextApiKey();
                retryWithDelay(prompt, content, FALLBACK_MODEL, freshKey, 0, 0, callback);
                return;
            }

            // Chiến lược 3: Retry với delay tăng dần
            if (attempt < MAX_RETRIES - 1) {
                retryWithDelay(prompt, content, model, currentKey, attempt + 1, keysTriedCount, callback);
                return;
            }
        }

        // Lỗi 500 (Server Error) → retry
        if (code == 500 && attempt < MAX_RETRIES - 1) {
            retryWithDelay(prompt, content, model, currentKey, attempt + 1, keysTriedCount, callback);
            return;
        }

        // Hết chiến lược → báo lỗi
        try {
            JSONObject errorJson = new JSONObject(responseBody);
            String msg = errorJson.getJSONObject("error").getString("message");
            callback.onError("Google AI: " + msg);
        } catch (Exception e) {
            callback.onError("Dịch vụ AI tạm thời không khả dụng (Lỗi " + code + "). Vui lòng thử lại sau.");
        }
    }

    /**
     * Retry với delay tăng dần: 2s, 4s, 8s (exponential backoff)
     */
    private void retryWithDelay(String prompt, String content, String model, String apiKey,
                                int attempt, int keysTriedCount, GeminiCallback callback) {
        long delayMs = (long) Math.pow(2, attempt + 1) * 1000;
        Log.d(TAG, "Retrying in " + delayMs + "ms...");

        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {}
            executeWithRetry(prompt, content, model, apiKey, attempt, keysTriedCount, callback);
        }).start();
    }
}
