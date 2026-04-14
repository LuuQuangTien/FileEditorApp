package hcmute.edu.vn.documentfileeditor.Service;

import android.util.Log;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service layer encapsulating translation business logic using Google ML Kit.
 * Decouples translation operations from Activity lifecycle.
 */
public class TranslateService {

    private static final String TAG = "TranslateService";

    private Translator currentTranslator;
    private String currentSourceLang;
    private String currentTargetLang;

    /**
     * Ordered map of display name → ML Kit language code.
     * Matches the languages supported by the text-recognition libraries in build.gradle.
     */
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

    /**
     * Returns the display names array for spinner adapters.
     */
    public String[] getLanguageNames() {
        return LANGUAGE_MAP.keySet().toArray(new String[0]);
    }

    /**
     * Translates text from sourceLanguage to targetLanguage.
     * Downloads the model if needed, then performs translation.
     *
     * @param sourceText     the text to translate
     * @param sourceLanguage display name of the source language (e.g. "English")
     * @param targetLanguage display name of the target language (e.g. "Vietnamese")
     * @param callback       result callback
     */
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

        // Reuse translator if same language pair, otherwise create new
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

        // Download model if needed, then translate
        DownloadConditions conditions = new DownloadConditions.Builder().build();
        callback.onModelDownloading();

        currentTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Model ready: " + sourceLanguage + " → " + targetLanguage);
                    currentTranslator.translate(sourceText)
                            .addOnSuccessListener(callback::onSuccess)
                            .addOnFailureListener(e -> callback.onFailure("Lỗi dịch: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure("Lỗi tải model ngôn ngữ: " + e.getMessage()));
    }

    /**
     * Closes the current translator to free resources.
     * Should be called when the Activity is destroyed.
     */
    public void closeTranslator() {
        if (currentTranslator != null) {
            currentTranslator.close();
            currentTranslator = null;
            currentSourceLang = null;
            currentTargetLang = null;
        }
    }

    /**
     * Callback interface for translation results.
     */
    public interface TranslateCallback {
        void onSuccess(String translatedText);
        void onFailure(String errorMessage);

        /**
         * Called when starting to download a translation model.
         * UI can show a loading indicator.
         */
        default void onModelDownloading() {}
    }
}
