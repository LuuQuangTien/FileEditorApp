package hcmute.edu.vn.documentfileeditor.Service;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiService {
    private static final String TAG = "GeminiService";

    private static final String[] API_KEYS;

    static {
        String raw = hcmute.edu.vn.documentfileeditor.BuildConfig.GEMINI_API_KEYS;
        if (raw != null && !raw.isEmpty()) {
            API_KEYS = raw.split(",");
        } else {
            API_KEYS = new String[]{};
        }
    }

    private static final AtomicInteger currentKeyIndex = new AtomicInteger(0);

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1/";
    private static final String DEFAULT_MODEL_PATH = "models/gemini-2.5-flash";
    private static final String PRIMARY_MODEL_PATH = DEFAULT_MODEL_PATH;
    private static final String FALLBACK_MODEL_PATH = DEFAULT_MODEL_PATH;
    private static final int MAX_RETRIES = 3;

    // Free tier is sensitive to bursts, so keep a shared global gap between requests.
    private static final long MIN_REQUEST_INTERVAL_MS = 4500L;
    private static final long DEFAULT_RATE_LIMIT_COOLDOWN_MS = 15000L;
    private static final Object REQUEST_THROTTLE_LOCK = new Object();
    private static long lastRequestStartedAtMs = 0L;
    private static long cooldownUntilMs = 0L;

    private final OkHttpClient client;

    private static final class QuotaStatus {
        final boolean dailyLimitExceeded;
        final boolean minuteLimitExceeded;
        final String rawMessage;

        QuotaStatus(boolean dailyLimitExceeded, boolean minuteLimitExceeded, String rawMessage) {
            this.dailyLimitExceeded = dailyLimitExceeded;
            this.minuteLimitExceeded = minuteLimitExceeded;
            this.rawMessage = rawMessage == null ? "" : rawMessage;
        }
    }

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

    private String getNextApiKey() {
        if (API_KEYS.length == 0) {
            return "";
        }
        int index = currentKeyIndex.getAndUpdate(i -> (i + 1) % API_KEYS.length);
        return API_KEYS[index].trim();
    }

    public void processText(String prompt, String content, GeminiCallback callback) {
        if (API_KEYS.length == 0) {
            callback.onError("Chua cau hinh API key. Vui long them GEMINI_API_KEYS trong local.properties");
            return;
        }
        executeWithRetry(prompt, content, PRIMARY_MODEL_PATH, getNextApiKey(), 0, 0, callback);
    }

    private void executeWithRetry(String prompt, String content, String model, String apiKey,
                                  int attempt, int keysTriedCount, GeminiCallback callback) {
        try {
            waitForAvailableRequestWindow();

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

            Log.d(TAG, "Requesting [model=" + model
                    + ", key=..." + apiKey.substring(Math.max(0, apiKey.length() - 4))
                    + ", attempt=" + (attempt + 1)
                    + ", keysTried=" + (keysTriedCount + 1) + "]");

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Network error: " + e.getMessage());
                    if (attempt < MAX_RETRIES - 1) {
                        retryWithDelay(prompt, content, model, apiKey, attempt + 1, keysTriedCount, callback);
                    } else {
                        callback.onError("Loi mang: " + e.getMessage());
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Response " + response.code() + " [model=" + model + "]");

                    if (response.isSuccessful()) {
                        clearCooldown();
                        handleSuccessResponse(responseBody, callback);
                    } else {
                        handleErrorResponse(response.code(), responseBody, prompt, content,
                                model, apiKey, attempt, keysTriedCount, callback, response);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Request build failed: " + e.getMessage());
            callback.onError("Khong the tao request.");
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

            callback.onTokenUsage(totalTokens);
            callback.onSuccess(result.trim());
        } catch (Exception e) {
            Log.e(TAG, "Parse error: " + e.getMessage());
            callback.onError("Loi phan tich phan hoi tu AI.");
        }
    }

    private void handleErrorResponse(int code, String responseBody, String prompt, String content,
                                     String model, String currentKey, int attempt, int keysTriedCount,
                                     GeminiCallback callback, Response response) {
        Log.w(TAG, "Error " + code + ": " + responseBody);

        if (code == 429 || code == 503) {
            long retryDelayMs = extractRetryDelayMs(responseBody, response, attempt);
            QuotaStatus quotaStatus = parseQuotaStatus(responseBody);
            applyCooldown(retryDelayMs);

            if (quotaStatus.dailyLimitExceeded) {
                callback.onError(buildQuotaExceededMessage(quotaStatus, retryDelayMs));
                return;
            }

            if (quotaStatus.minuteLimitExceeded) {
                if (attempt < MAX_RETRIES - 1) {
                    retryWithDelay(prompt, content, model, currentKey, attempt + 1, keysTriedCount,
                            callback, retryDelayMs);
                    return;
                }

                callback.onError(buildQuotaExceededMessage(quotaStatus, retryDelayMs));
                return;
            }

            if (keysTriedCount < API_KEYS.length - 1) {
                String nextKey = getNextApiKey();
                Log.d(TAG, "Switching to next API key...");
                retryWithDelay(prompt, content, model, nextKey, 0, keysTriedCount + 1, callback);
                return;
            }

            if (PRIMARY_MODEL_PATH.equals(model)) {
                Log.d(TAG, "All keys exhausted for " + PRIMARY_MODEL_PATH + ", switching to " + FALLBACK_MODEL_PATH);
                String freshKey = getNextApiKey();
                retryWithDelay(prompt, content, FALLBACK_MODEL_PATH, freshKey, 0, 0, callback);
                return;
            }

            if (attempt < MAX_RETRIES - 1) {
                retryWithDelay(prompt, content, model, currentKey, attempt + 1, keysTriedCount, callback);
                return;
            }
        }

        if (code == 500 && attempt < MAX_RETRIES - 1) {
            retryWithDelay(prompt, content, model, currentKey, attempt + 1, keysTriedCount, callback);
            return;
        }

        try {
            JSONObject errorJson = new JSONObject(responseBody);
            String msg = errorJson.getJSONObject("error").getString("message");
            callback.onError(mapFriendlyErrorMessage(code, msg));
        } catch (Exception e) {
            callback.onError("Dich vu AI tam thoi khong kha dung (Loi " + code + "). Vui long thu lai sau.");
        }
    }

    private String mapFriendlyErrorMessage(int code, String rawMessage) {
        String message = rawMessage == null ? "" : rawMessage.trim();
        String lowerMessage = message.toLowerCase(Locale.US);

        if (code == 429
                || lowerMessage.contains("quota")
                || lowerMessage.contains("rate limit")
                || lowerMessage.contains("resource has been exhausted")) {
            return "API AI dang cham do free tier bi gioi han tan suat. Ung dung se tu gian nhip request, vui long thu lai sau it giay.";
        }

        if (lowerMessage.contains("token")
                && (lowerMessage.contains("per minute")
                || lowerMessage.contains("rate limit")
                || lowerMessage.contains("quota")
                || lowerMessage.contains("resource has been exhausted"))) {
            return "API AI dang cham do free tier bi gioi han tan suat. Ung dung se tu gian nhip request, vui long thu lai sau it giay.";
        }

        if (lowerMessage.contains("token")
                || lowerMessage.contains("context")
                || lowerMessage.contains("too long")
                || lowerMessage.contains("maximum input")) {
            return "Noi dung gui den AI qua dai. Hay chon mot doan ngan hon hoac thu lai voi van ban it hon.";
        }

        return "Google AI: " + message;
    }

    private QuotaStatus parseQuotaStatus(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return new QuotaStatus(false, false, "");
        }

        boolean dailyLimitExceeded = false;
        boolean minuteLimitExceeded = false;
        String rawMessage = "";

        try {
            JSONObject root = new JSONObject(responseBody);
            JSONObject error = root.optJSONObject("error");
            if (error == null) {
                return new QuotaStatus(false, false, "");
            }

            rawMessage = error.optString("message", "");
            JSONArray details = error.optJSONArray("details");
            if (details != null) {
                for (int i = 0; i < details.length(); i++) {
                    JSONObject detail = details.optJSONObject(i);
                    if (detail == null) {
                        continue;
                    }

                    JSONArray violations = detail.optJSONArray("violations");
                    if (violations == null) {
                        continue;
                    }

                    for (int j = 0; j < violations.length(); j++) {
                        JSONObject violation = violations.optJSONObject(j);
                        if (violation == null) {
                            continue;
                        }

                        String quotaId = violation.optString("quotaId", "").toLowerCase(Locale.US);
                        String quotaMetric = violation.optString("quotaMetric", "").toLowerCase(Locale.US);
                        if (quotaId.contains("perday") || quotaMetric.contains("perday")) {
                            dailyLimitExceeded = true;
                        }
                        if (quotaId.contains("perminute") || quotaMetric.contains("perminute")) {
                            minuteLimitExceeded = true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not parse quota status: " + e.getMessage());
        }

        String lowerMessage = rawMessage.toLowerCase(Locale.US);
        if (lowerMessage.contains("per day")) {
            dailyLimitExceeded = true;
        }
        if (lowerMessage.contains("per minute") || lowerMessage.contains("retry in")) {
            minuteLimitExceeded = true;
        }

        return new QuotaStatus(dailyLimitExceeded, minuteLimitExceeded, rawMessage);
    }

    private String buildQuotaExceededMessage(QuotaStatus quotaStatus, long retryDelayMs) {
        if (quotaStatus.dailyLimitExceeded) {
            return "Gemini free tier da het quota theo ngay cho project/model nay. Thu lai vao ngay mai hoac doi API key/project khac.";
        }

        if (quotaStatus.minuteLimitExceeded) {
            long seconds = Math.max(1L, (retryDelayMs + 999L) / 1000L);
            return "Gemini free tier dang vuot gioi han theo phut. Vui long doi khoang "
                    + seconds + " giay roi thu lai.";
        }

        return mapFriendlyErrorMessage(429, quotaStatus.rawMessage);
    }

    private void waitForAvailableRequestWindow() {
        while (true) {
            long waitMs;
            synchronized (REQUEST_THROTTLE_LOCK) {
                long now = System.currentTimeMillis();
                long nextAllowedAt = Math.max(lastRequestStartedAtMs + MIN_REQUEST_INTERVAL_MS, cooldownUntilMs);
                waitMs = nextAllowedAt - now;
                if (waitMs <= 0) {
                    lastRequestStartedAtMs = now;
                    return;
                }
            }

            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void applyCooldown(long cooldownMs) {
        synchronized (REQUEST_THROTTLE_LOCK) {
            long until = System.currentTimeMillis() + Math.max(cooldownMs, MIN_REQUEST_INTERVAL_MS);
            cooldownUntilMs = Math.max(cooldownUntilMs, until);
        }
    }

    private void clearCooldown() {
        synchronized (REQUEST_THROTTLE_LOCK) {
            if (cooldownUntilMs <= System.currentTimeMillis()) {
                cooldownUntilMs = 0L;
            }
        }
    }

    private long extractRetryDelayMs(String responseBody, Response response, int attempt) {
        long retryAfterHeaderMs = parseRetryAfterHeader(response);
        if (retryAfterHeaderMs > 0) {
            return retryAfterHeaderMs;
        }

        long bodyDelayMs = parseRetryDelayFromErrorBody(responseBody);
        if (bodyDelayMs > 0) {
            return bodyDelayMs;
        }

        return Math.max(DEFAULT_RATE_LIMIT_COOLDOWN_MS, (long) Math.pow(2, attempt + 2) * 1000L);
    }

    private long parseRetryAfterHeader(Response response) {
        if (response == null) {
            return -1L;
        }

        String retryAfter = response.header("Retry-After");
        if (retryAfter == null || retryAfter.trim().isEmpty()) {
            return -1L;
        }

        try {
            return Long.parseLong(retryAfter.trim()) * 1000L;
        } catch (NumberFormatException e) {
            Log.w(TAG, "Could not parse Retry-After header: " + retryAfter);
            return -1L;
        }
    }

    private long parseRetryDelayFromErrorBody(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return -1L;
        }

        try {
            JSONObject root = new JSONObject(responseBody);
            JSONObject error = root.optJSONObject("error");
            if (error == null) {
                return -1L;
            }

            JSONArray details = error.optJSONArray("details");
            if (details == null) {
                return -1L;
            }

            for (int i = 0; i < details.length(); i++) {
                JSONObject detail = details.optJSONObject(i);
                if (detail == null) {
                    continue;
                }

                String retryDelay = detail.optString("retryDelay", "");
                long parsed = parseDurationMillis(retryDelay);
                if (parsed > 0) {
                    return parsed;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not parse retry delay from error body: " + e.getMessage());
        }

        return -1L;
    }

    private long parseDurationMillis(String durationText) {
        if (durationText == null) {
            return -1L;
        }

        String normalized = durationText.trim().toLowerCase(Locale.US);
        if (normalized.isEmpty() || !normalized.endsWith("s")) {
            return -1L;
        }

        try {
            double seconds = Double.parseDouble(normalized.substring(0, normalized.length() - 1));
            return (long) (seconds * 1000L);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private void retryWithDelay(String prompt, String content, String model, String apiKey,
                                int attempt, int keysTriedCount, GeminiCallback callback) {
        long delayMs = (long) Math.pow(2, attempt + 1) * 1000;
        retryWithDelay(prompt, content, model, apiKey, attempt, keysTriedCount, callback, delayMs);
    }

    private void retryWithDelay(String prompt, String content, String model, String apiKey,
                                int attempt, int keysTriedCount, GeminiCallback callback, long delayMs) {
        Log.d(TAG, "Retrying in " + delayMs + "ms...");

        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            executeWithRetry(prompt, content, model, apiKey, attempt, keysTriedCount, callback);
        }).start();
    }
}
