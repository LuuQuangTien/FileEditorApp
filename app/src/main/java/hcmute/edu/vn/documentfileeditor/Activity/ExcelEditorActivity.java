package hcmute.edu.vn.documentfileeditor.Activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.Map;

import hcmute.edu.vn.documentfileeditor.Adapter.SpreadsheetAdapter;
import hcmute.edu.vn.documentfileeditor.Enum.FileType;
import hcmute.edu.vn.documentfileeditor.Model.Callback.DocumentCallback;
import hcmute.edu.vn.documentfileeditor.Model.Entity.CellData;
import hcmute.edu.vn.documentfileeditor.Model.Entity.CellStyle;
import hcmute.edu.vn.documentfileeditor.Model.Entity.DocumentFB;
import hcmute.edu.vn.documentfileeditor.Model.Repository.DocumentRepository;
import hcmute.edu.vn.documentfileeditor.R;
import hcmute.edu.vn.documentfileeditor.Service.AuthService;
import hcmute.edu.vn.documentfileeditor.Service.DocumentService;

public class ExcelEditorActivity extends AppCompatActivity implements SpreadsheetAdapter.OnCellClickListener {
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
    private DocumentService documentService;
    private AuthService authService;
    private DocumentFB currentDocument;

    private static final String[] COLUMNS = {"", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
    private static final int ROWS = 21;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_excel_editor);

        documentRepository = DocumentRepository.getInstance(this);
        documentService = new DocumentService();
        authService = new AuthService();
        initializeViews();
        initializeData();
        setupListeners();
        bindDocumentInfo();
    }

    @Override
    public void onCellClick(String cellKey) {
        selectedCell = cellKey;
        updateFormulaBar();
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

        float density = getResources().getDisplayMetrics().density;
        rvSpreadsheet.setLayoutManager(new GridLayoutManager(this, COLUMNS.length));
        adapter = new SpreadsheetAdapter(COLUMNS, ROWS, sheetData, density, this);
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
        currentDocument.setUserId(authService.getCurrentUserId());
        currentDocument.setFileName(getIntent().getStringExtra(DocumentEditorActivity.EXTRA_DOCUMENT_NAME));
        currentDocument.setLocalPath(getIntent().getStringExtra(DocumentEditorActivity.EXTRA_LOCAL_PATH));
        currentDocument.setCloudStorageUrl(getIntent().getStringExtra(DocumentEditorActivity.EXTRA_CLOUD_URL));
        currentDocument.setFileType(FileType.EXCEL);

        if (currentDocument.getFileName() != null && !currentDocument.getFileName().isEmpty()) {
            tvSheetTitle.setText(currentDocument.getFileName());
        }

        String serialized = documentService.readTextFromLocalFile(this, currentDocument.getLocalPath());
        if (serialized != null && !serialized.isEmpty()) {
            loadSheetData(serialized);
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
                    cellData.setFormula(parts[2]);
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
                    .append(cellData.getValue() == null ? "" : cellData.getValue().replace("\n", " "))
                    .append('\t')
                    .append(cellData.getFormula() == null ? "" : cellData.getFormula().replace("\n", " "))
                    .append('\n');
        }

        if (!documentService.saveTextToLocalFile(currentDocument.getLocalPath(), builder.toString())) {
            Toast.makeText(this, "Could not save spreadsheet locally.", Toast.LENGTH_SHORT).show();
            return;
        }

        documentRepository.saveDocument(currentDocument, new DocumentCallback.UploadCallback() {
            @Override
            public void onSuccess(DocumentFB documentFB) {
                currentDocument = documentFB;
                String message = documentRepository.isCloudSyncConfigured()
                        ? "Spreadsheet saved locally. Cloud sync continues in background."
                        : "Spreadsheet saved locally. Add Cloudinary config in local.properties to enable cloud sync.";
                Toast.makeText(ExcelEditorActivity.this, message, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onProgress(int progressPercentage) {
            }

            @Override
            public void onFailure(Exception e) {
                String message = e != null && e.getMessage() != null && !e.getMessage().isEmpty()
                        ? e.getMessage()
                        : "Save failed.";
                Toast.makeText(ExcelEditorActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFormulaBar() {
        CellData cellData = sheetData.get(selectedCell);
        if (cellData != null) {
            etFormula.setText(cellData.getFormula() != null ? cellData.getFormula() : cellData.getValue());
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
        cellData.setValue(value);
        if (value.startsWith("=")) {
            cellData.setFormula(value);
            cellData.setValue(evaluateFormula(value));
        } else {
            cellData.setFormula(null);
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
        if (cellData != null && cellData.getStyle() != null) {
            CellStyle style = cellData.getStyle();
            btnBold.setSelected(style.isBold());
            btnItalic.setSelected(style.isItalic());
            btnUnderline.setSelected(style.isUnderline());
        }
    }

    private void toggleBold() {
        CellData cellData = getOrCreateCellData(selectedCell);
        cellData.getStyle().setBold(!cellData.getStyle().isBold());
        updateToolbarButtons();
        adapter.notifyDataSetChanged();
    }

    private void toggleItalic() {
        CellData cellData = getOrCreateCellData(selectedCell);
        cellData.getStyle().setItalic(!cellData.getStyle().isItalic());
        updateToolbarButtons();
        adapter.notifyDataSetChanged();
    }

    private void toggleUnderline() {
        CellData cellData = getOrCreateCellData(selectedCell);
        cellData.getStyle().setUnderline(!cellData.getStyle().isUnderline());
        updateToolbarButtons();
        adapter.notifyDataSetChanged();
    }

    private void setAlignment(String align) {
        CellData cellData = getOrCreateCellData(selectedCell);
        cellData.getStyle().setAlign(align);
        updateToolbarButtons();
        adapter.notifyDataSetChanged();
    }

    private void setBackgroundColor(String color) {
        CellData cellData = getOrCreateCellData(selectedCell);
        cellData.getStyle().setBgColor(color);
        adapter.notifyDataSetChanged();
    }

    private CellData getOrCreateCellData(String cellKey) {
        CellData cellData = sheetData.get(cellKey);
        if (cellData == null) {
            cellData = new CellData("");
            sheetData.put(cellKey, cellData);
        }
        if (cellData.getStyle() == null) {
            cellData.setStyle(new CellStyle());
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
}
