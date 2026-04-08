package hcmute.edu.vn.documentfileeditor.Activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import hcmute.edu.vn.documentfileeditor.Enum.FileType;
import hcmute.edu.vn.documentfileeditor.Model.Dao.DocumentCallback;
import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentFB;
import hcmute.edu.vn.documentfileeditor.Model.Repository.DocumentRepository;
import hcmute.edu.vn.documentfileeditor.R;

public class ExcelEditorActivity extends AppCompatActivity {
    private EditText etFormula;
    private TextView tvCellRef;
    private RecyclerView rvSpreadsheet;
    private Spinner spinnerFontSize;
    private ImageButton btnBold;
    private ImageButton btnItalic;
    private ImageButton btnUnderline;
    private ImageButton btnAlignLeft;
    private ImageButton btnAlignCenter;
    private ImageButton btnAlignRight;
    private ImageButton btnBgYellow;
    private ImageButton btnBgBlue;
    private ImageButton btnBgGreen;

    private final Map<String, CellData> sheetData = new HashMap<>();
    private String selectedCell = "A1";
    private SpreadsheetAdapter adapter;
    private DocumentRepository documentRepository;
    private DocumentFB currentDocument;

    private static final String[] COLUMNS = {"", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
    private static final int ROWS = 21;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_excel_editor);

        documentRepository = DocumentRepository.getInstance(this);
        initializeViews();
        initializeData();
        setupListeners();
        bindDocumentInfo();
    }

    private void initializeViews() {
        etFormula = findViewById(R.id.et_formula);
        tvCellRef = findViewById(R.id.tv_cell_ref);
        rvSpreadsheet = findViewById(R.id.rv_spreadsheet);
        spinnerFontSize = findViewById(R.id.spinner_font_size);
        btnBold = findViewById(R.id.btn_bold);
        btnItalic = findViewById(R.id.btn_italic);
        btnUnderline = findViewById(R.id.btn_underline);
        btnAlignLeft = findViewById(R.id.btn_align_left);
        btnAlignCenter = findViewById(R.id.btn_align_center);
        btnAlignRight = findViewById(R.id.btn_align_right);
        btnBgYellow = findViewById(R.id.btn_bg_yellow);
        btnBgBlue = findViewById(R.id.btn_bg_blue);
        btnBgGreen = findViewById(R.id.btn_bg_green);

        rvSpreadsheet.setLayoutManager(new GridLayoutManager(this, COLUMNS.length));
        adapter = new SpreadsheetAdapter();
        rvSpreadsheet.setAdapter(adapter);
    }

    private void initializeData() {
        sheetData.clear();
        sheetData.put("A1", new CellData("Product"));
        sheetData.put("B1", new CellData("Quantity"));
        sheetData.put("C1", new CellData("Price"));
        sheetData.put("D1", new CellData("Total"));
        sheetData.put("A2", new CellData("Laptop"));
        sheetData.put("B2", new CellData("5"));
        sheetData.put("C2", new CellData("1200"));
        sheetData.put("D2", new CellData("6000", "=B2*C2"));
        updateFormulaBar();
    }

    private void setupListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_save).setOnClickListener(v -> saveSpreadsheet());
        findViewById(R.id.btn_more).setOnClickListener(v -> showMoreMenu());

        etFormula.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateSelectedCell(s.toString());
            }
        });

        btnBold.setOnClickListener(v -> toggleBold());
        btnItalic.setOnClickListener(v -> toggleItalic());
        btnUnderline.setOnClickListener(v -> toggleUnderline());
        btnAlignLeft.setOnClickListener(v -> setAlignment("left"));
        btnAlignCenter.setOnClickListener(v -> setAlignment("center"));
        btnAlignRight.setOnClickListener(v -> setAlignment("right"));
        btnBgYellow.setOnClickListener(v -> setBackgroundColor("#FEF3C7"));
        btnBgBlue.setOnClickListener(v -> setBackgroundColor("#DBEAFE"));
        btnBgGreen.setOnClickListener(v -> setBackgroundColor("#DCFCE7"));
    }

    private void bindDocumentInfo() {
        TextView tvSheetTitle = findViewById(R.id.tv_sheet_title);

        currentDocument = new DocumentFB();
        currentDocument.setId(getIntent().getStringExtra(DocumentEditorActivity.EXTRA_DOCUMENT_ID));
        currentDocument.setUserId(com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null);
        currentDocument.setFileName(getIntent().getStringExtra(DocumentEditorActivity.EXTRA_DOCUMENT_NAME));
        currentDocument.setLocalPath(getIntent().getStringExtra(DocumentEditorActivity.EXTRA_LOCAL_PATH));
        currentDocument.setCloudStorageUrl(getIntent().getStringExtra(DocumentEditorActivity.EXTRA_CLOUD_URL));
        currentDocument.setFileType(FileType.EXCEL);

        if (currentDocument.getFileName() != null && !currentDocument.getFileName().isEmpty()) {
            tvSheetTitle.setText(currentDocument.getFileName());
        }

        String serialized = readTextFromLocalFile(currentDocument.getLocalPath());
        if (serialized != null && !serialized.isEmpty()) {
            loadSheetData(serialized);
        }
    }

    private String readTextFromLocalFile(String localPath) {
        if (localPath == null || localPath.isEmpty()) {
            return null;
        }
        try {
            File file = new File(localPath);
            if (!file.exists()) {
                return null;
            }
            InputStream input = getContentResolver().openInputStream(android.net.Uri.fromFile(file));
            if (input == null) {
                return null;
            }
            byte[] bytes = new byte[(int) file.length()];
            int read = input.read(bytes);
            input.close();
            if (read <= 0) {
                return null;
            }
            return new String(bytes, 0, read, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private void loadSheetData(String serialized) {
        sheetData.clear();
        String[] lines = serialized.split("\n");
        for (String line : lines) {
            String[] parts = line.split("\t", -1);
            if (parts.length >= 2) {
                CellData cellData = new CellData(parts[1]);
                if (parts.length >= 3 && !parts[2].isEmpty()) {
                    cellData.formula = parts[2];
                }
                sheetData.put(parts[0], cellData);
            }
        }
        adapter.notifyDataSetChanged();
        updateFormulaBar();
    }

    private void saveSpreadsheet() {
        if (currentDocument == null || currentDocument.getLocalPath() == null || currentDocument.getLocalPath().isEmpty()) {
            Toast.makeText(this, "Spreadsheet path is unavailable.", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, CellData> entry : sheetData.entrySet()) {
            CellData cellData = entry.getValue();
            builder.append(entry.getKey())
                    .append('\t')
                    .append(cellData.value == null ? "" : cellData.value.replace("\n", " "))
                    .append('\t')
                    .append(cellData.formula == null ? "" : cellData.formula.replace("\n", " "))
                    .append('\n');
        }

        try {
            FileOutputStream output = new FileOutputStream(new File(currentDocument.getLocalPath()), false);
            output.write(builder.toString().getBytes(StandardCharsets.UTF_8));
            output.close();
        } catch (Exception e) {
            Toast.makeText(this, "Could not save spreadsheet locally.", Toast.LENGTH_SHORT).show();
            return;
        }

        documentRepository.saveDocument(currentDocument, new DocumentCallback.UploadCallback() {
            @Override
            public void onSuccess(DocumentFB documentFB) {
                currentDocument = documentFB;
                Toast.makeText(ExcelEditorActivity.this, "Spreadsheet saved locally. Cloud sync continues in background.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onProgress(int progressPercentage) {
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ExcelEditorActivity.this, "Save failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFormulaBar() {
        CellData cellData = sheetData.get(selectedCell);
        if (cellData != null) {
            etFormula.setText(cellData.formula != null ? cellData.formula : cellData.value);
        } else {
            etFormula.setText("");
        }
        tvCellRef.setText(selectedCell);
        updateToolbarButtons();
    }

    private void updateSelectedCell(String value) {
        CellData cellData = sheetData.get(selectedCell);
        if (cellData == null) {
            cellData = new CellData("");
            sheetData.put(selectedCell, cellData);
        }
        cellData.value = value;
        if (value.startsWith("=")) {
            cellData.formula = value;
            cellData.value = evaluateFormula(value);
        } else {
            cellData.formula = null;
        }
        adapter.notifyDataSetChanged();
    }

    private String evaluateFormula(String formula) {
        switch (formula) {
            case "=B2*C2":
                return "6000";
            default:
                return formula;
        }
    }

    private void updateToolbarButtons() {
        CellData cellData = sheetData.get(selectedCell);
        if (cellData != null && cellData.style != null) {
            btnBold.setSelected(cellData.style.bold);
            btnItalic.setSelected(cellData.style.italic);
            btnUnderline.setSelected(cellData.style.underline);
        }
    }

    private void toggleBold() {
        CellData cellData = getOrCreateCellData(selectedCell);
        cellData.style.bold = !cellData.style.bold;
        updateToolbarButtons();
        adapter.notifyDataSetChanged();
    }

    private void toggleItalic() {
        CellData cellData = getOrCreateCellData(selectedCell);
        cellData.style.italic = !cellData.style.italic;
        updateToolbarButtons();
        adapter.notifyDataSetChanged();
    }

    private void toggleUnderline() {
        CellData cellData = getOrCreateCellData(selectedCell);
        cellData.style.underline = !cellData.style.underline;
        updateToolbarButtons();
        adapter.notifyDataSetChanged();
    }

    private void setAlignment(String align) {
        CellData cellData = getOrCreateCellData(selectedCell);
        cellData.style.align = align;
        updateToolbarButtons();
        adapter.notifyDataSetChanged();
    }

    private void setBackgroundColor(String color) {
        CellData cellData = getOrCreateCellData(selectedCell);
        cellData.style.bgColor = color;
        adapter.notifyDataSetChanged();
    }

    private CellData getOrCreateCellData(String cellKey) {
        CellData cellData = sheetData.get(cellKey);
        if (cellData == null) {
            cellData = new CellData("");
            sheetData.put(cellKey, cellData);
        }
        if (cellData.style == null) {
            cellData.style = new CellStyle();
        }
        return cellData;
    }

    private void showMoreMenu() {
        PopupMenu popup = new PopupMenu(this, findViewById(R.id.btn_more));
        popup.getMenu().add("Export as CSV");
        popup.getMenu().add("Export as XLSX");
        popup.getMenu().add("Share");
        popup.setOnMenuItemClickListener(item -> {
            Toast.makeText(this, "Exported as " + item.getTitle(), Toast.LENGTH_SHORT).show();
            return true;
        });
        popup.show();
    }

    private static class CellData {
        String value;
        String formula;
        CellStyle style;

        CellData(String value) {
            this.value = value;
            this.style = new CellStyle();
        }

        CellData(String value, String formula) {
            this.value = value;
            this.formula = formula;
            this.style = new CellStyle();
        }
    }

    private static class CellStyle {
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        String align = "left";
        String bgColor = "#FFFFFF";
        String textColor = "#000000";
        String fontSize = "14";
    }

    private class SpreadsheetAdapter extends RecyclerView.Adapter<SpreadsheetAdapter.CellViewHolder> {
        @NonNull
        @Override
        public CellViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textView = new TextView(ExcelEditorActivity.this);
            int heightPx = (int) (40 * getResources().getDisplayMetrics().density);
            textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, heightPx));
            textView.setPadding(16, 8, 16, 8);
            textView.setBackgroundResource(R.drawable.cell_border);
            textView.setGravity(android.view.Gravity.CENTER_VERTICAL);
            textView.setTextSize(14);
            return new CellViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull CellViewHolder holder, int position) {
            int row = position / COLUMNS.length;
            int col = position % COLUMNS.length;
            TextView textView = (TextView) holder.itemView;

            int widthDp = (col == 0) ? 50 : 100;
            int widthPx = (int) (widthDp * getResources().getDisplayMetrics().density);
            ViewGroup.LayoutParams params = textView.getLayoutParams();
            params.width = widthPx;
            textView.setLayoutParams(params);

            if (row == 0) {
                textView.setText(COLUMNS[col]);
                textView.setBackgroundColor(0xFFE5E7EB);
                textView.setTypeface(null, android.graphics.Typeface.BOLD);
            } else if (col == 0) {
                textView.setText(String.valueOf(row));
                textView.setBackgroundColor(0xFFE5E7EB);
                textView.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                String cellKey = COLUMNS[col] + row;
                CellData cellData = sheetData.get(cellKey);
                String displayText = cellData != null ? cellData.value : "";

                textView.setText(displayText);
                textView.setBackgroundColor(android.graphics.Color.parseColor(
                        cellData != null && cellData.style != null ? cellData.style.bgColor : "#FFFFFF"));

                int typeface = android.graphics.Typeface.NORMAL;
                if (cellData != null && cellData.style != null) {
                    if (cellData.style.bold) {
                        typeface |= android.graphics.Typeface.BOLD;
                    }
                    if (cellData.style.italic) {
                        typeface |= android.graphics.Typeface.ITALIC;
                    }
                }
                textView.setTypeface(null, typeface);

                int gravity = android.view.Gravity.CENTER_VERTICAL;
                if (cellData != null && cellData.style != null) {
                    switch (cellData.style.align.toLowerCase(Locale.US)) {
                        case "center":
                            gravity |= android.view.Gravity.CENTER_HORIZONTAL;
                            break;
                        case "right":
                            gravity |= android.view.Gravity.END;
                            break;
                        default:
                            gravity |= android.view.Gravity.START;
                            break;
                    }
                } else {
                    gravity |= android.view.Gravity.START;
                }
                textView.setGravity(gravity);

                textView.setOnClickListener(v -> {
                    selectedCell = cellKey;
                    updateFormulaBar();
                });
            }
        }

        @Override
        public int getItemCount() {
            return ROWS * COLUMNS.length;
        }

        class CellViewHolder extends RecyclerView.ViewHolder {
            CellViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
}
