package hcmute.edu.vn.documentfileeditor.Activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import hcmute.edu.vn.documentfileeditor.R;
import java.util.*;

public class ExcelEditorActivity extends AppCompatActivity {

    private EditText etFormula;
    private TextView tvCellRef;
    private RecyclerView rvSpreadsheet;
    private Spinner spinnerFontSize;
    private ImageButton btnBold, btnItalic, btnUnderline;
    private ImageButton btnAlignLeft, btnAlignCenter, btnAlignRight;
    private ImageButton btnBgYellow, btnBgBlue, btnBgGreen;

    private final Map<String, CellData> sheetData = new HashMap<>();
    private String selectedCell = "A1";
    private SpreadsheetAdapter adapter;

    private static final String[] COLUMNS = {"", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
    private static final int ROWS = 21; // 20 data rows + 1 header

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_excel_editor);

        initializeViews();
        initializeData();
        setupListeners();
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
        // Initialize sample data
        sheetData.put("A1", new CellData("Product"));
        sheetData.put("B1", new CellData("Quantity"));
        sheetData.put("C1", new CellData("Price"));
        sheetData.put("D1", new CellData("Total"));
        sheetData.put("A2", new CellData("Laptop"));
        sheetData.put("B2", new CellData("5"));
        sheetData.put("C2", new CellData("1200"));
        sheetData.put("D2", new CellData("6000", "=B2*C2"));
        sheetData.put("A3", new CellData("Mouse"));
        sheetData.put("B3", new CellData("15"));
        sheetData.put("C3", new CellData("25"));
        sheetData.put("D3", new CellData("375", "=B3*C3"));
        sheetData.put("A4", new CellData("Keyboard"));
        sheetData.put("B4", new CellData("10"));
        sheetData.put("C4", new CellData("75"));
        sheetData.put("D4", new CellData("750", "=B4*C4"));

        updateFormulaBar();
    }

    private void setupListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_save).setOnClickListener(v ->
            Toast.makeText(this, "Spreadsheet saved!", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btn_more).setOnClickListener(v -> showMoreMenu());

        etFormula.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateSelectedCell(s.toString());
            }
        });

        // Formatting buttons
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
            // Simple formula evaluation (just for demo)
            cellData.value = evaluateFormula(value);
        }
        adapter.notifyDataSetChanged();
    }

    private String evaluateFormula(String formula) {
        // Very basic formula evaluation for demo
        switch (formula) {
            case "=B2*C2":
                return "6000";
            case "=B3*C3":
                return "375";
            case "=B4*C4":
                return "750";
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
            // Update alignment buttons based on style.align
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

    // Cell data model
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

    // RecyclerView Adapter
    private class SpreadsheetAdapter extends RecyclerView.Adapter<SpreadsheetAdapter.CellViewHolder> {

        @NonNull
        @Override
        public CellViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textView = new TextView(ExcelEditorActivity.this);
            // Height 40dp approx
            int heightPx = (int) (40 * getResources().getDisplayMetrics().density);
            textView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, heightPx));
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
            
            // Set dynamic width based on whether it is a row header
            int widthDp = (col == 0) ? 50 : 100;
            int widthPx = (int) (widthDp * getResources().getDisplayMetrics().density);
            ViewGroup.LayoutParams params = textView.getLayoutParams();
            if (params != null) {
                params.width = widthPx;
                textView.setLayoutParams(params);
            } else {
                textView.setLayoutParams(new ViewGroup.LayoutParams(widthPx, 
                    (int) (40 * getResources().getDisplayMetrics().density)));
            }

            if (row == 0) {
                // Header row
                textView.setText(COLUMNS[col]);
                textView.setBackgroundColor(0xFFE5E7EB);
                textView.setTypeface(null, android.graphics.Typeface.BOLD);
            } else if (col == 0) {
                // Row numbers
                textView.setText(String.valueOf(row));
                textView.setBackgroundColor(0xFFE5E7EB);
                textView.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                // Data cells
                String cellKey = COLUMNS[col] + row;
                CellData cellData = sheetData.get(cellKey);
                String displayText = cellData != null ? cellData.value : "";

                textView.setText(displayText);
                textView.setBackgroundColor(android.graphics.Color.parseColor(
                    cellData != null && cellData.style != null ? cellData.style.bgColor : "#FFFFFF"));

                // Apply text style
                int typeface = android.graphics.Typeface.NORMAL;
                if (cellData != null && cellData.style != null) {
                    if (cellData.style.bold) typeface |= android.graphics.Typeface.BOLD;
                    if (cellData.style.italic) typeface |= android.graphics.Typeface.ITALIC;
                }
                textView.setTypeface(null, typeface);

                // Apply alignment
                int gravity = android.view.Gravity.CENTER_VERTICAL;
                if (cellData != null && cellData.style != null) {
                    switch (cellData.style.align) {
                        case "center": gravity |= android.view.Gravity.CENTER_HORIZONTAL; break;
                        case "right": gravity |= android.view.Gravity.END; break;
                        default: gravity |= android.view.Gravity.START; break;
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