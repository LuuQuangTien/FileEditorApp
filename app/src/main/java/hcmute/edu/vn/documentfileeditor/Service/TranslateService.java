package hcmute.edu.vn.documentfileeditor.Service;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.LinkedHashMap;
import java.util.Map;

public class TranslateService {

    private static final String TAG = "TranslateService";
    private static final long MODEL_DOWNLOAD_TOAST_DELAY_MS = 500;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Translator currentTranslator;
    private String currentSourceLang;
    private String currentTargetLang;

    private static final Map<String, String> LANGUAGE_MAP = new LinkedHashMap<>();

    static {
        LANGUAGE_MAP.put("English", TranslateLanguage.ENGLISH);
        LANGUAGE_MAP.put("Vietnamese", TranslateLanguage.VIETNAMESE);
        LANGUAGE_MAP.put("Chinese", TranslateLanguage.CHINESE);
        LANGUAGE_MAP.put("Japanese", TranslateLanguage.JAPANESE);
        LANGUAGE_MAP.put("Korean", TranslateLanguage.KOREAN);
        LANGUAGE_MAP.put("Spanish", TranslateLanguage.SPANISH);
        LANGUAGE_MAP.put("French", TranslateLanguage.FRENCH);
        LANGUAGE_MAP.put("German", TranslateLanguage.GERMAN);
    }

    public String[] getLanguageNames() {
        return LANGUAGE_MAP.keySet().toArray(new String[0]);
    }

    public void translate(String sourceText, String sourceLanguage, String targetLanguage,
                          TranslateCallback callback) {
        if (sourceText == null || sourceText.trim().isEmpty()) {
            callback.onFailure("Vui lòng nhập văn bản cần dịch");
            return;
        }

        String sourceLangCode = LANGUAGE_MAP.get(sourceLanguage);
        String targetLangCode = LANGUAGE_MAP.get(targetLanguage);

        if (sourceLangCode == null || targetLangCode == null) {
            callback.onFailure("Ngôn ngữ không được hỗ trợ");
            return;
        }

        if (sourceLangCode.equals(targetLangCode)) {
            callback.onFailure("Ngôn ngữ nguồn và đích phải khác nhau");
            return;
        }

        if (currentTranslator == null
                || !sourceLangCode.equals(currentSourceLang)
                || !targetLangCode.equals(currentTargetLang)) {
            closeTranslator();

            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLangCode)
                    .setTargetLanguage(targetLangCode)
                    .build();
            currentTranslator = Translation.getClient(options);
            currentSourceLang = sourceLangCode;
            currentTargetLang = targetLangCode;
        }

        DownloadConditions conditions = new DownloadConditions.Builder().build();

        Runnable downloadingNotification = callback::onModelDownloading;
        mainHandler.postDelayed(downloadingNotification, MODEL_DOWNLOAD_TOAST_DELAY_MS);

        currentTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    mainHandler.removeCallbacks(downloadingNotification);
                    Log.d(TAG, "Model ready: " + sourceLanguage + " → " + targetLanguage);
                    currentTranslator.translate(sourceText)
                            .addOnSuccessListener(callback::onSuccess)
                            .addOnFailureListener(e -> callback.onFailure("Lỗi dịch: " + e.getMessage()));
                })
                .addOnFailureListener(e -> {
                    mainHandler.removeCallbacks(downloadingNotification);
                    callback.onFailure("Lỗi tải model ngôn ngữ: " + e.getMessage());
                });
    }

    public void closeTranslator() {
        if (currentTranslator != null) {
            currentTranslator.close();
            currentTranslator = null;
            currentSourceLang = null;
            currentTargetLang = null;
        }
    }

    public interface TranslateCallback {
        void onSuccess(String translatedText);
        void onFailure(String errorMessage);
        default void onModelDownloading() {}
    }
}
