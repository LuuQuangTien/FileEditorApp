package hcmute.edu.vn.documentfileeditor.Adapter;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Locale;
import java.util.Map;

import hcmute.edu.vn.documentfileeditor.Model.Entity.CellData;
import hcmute.edu.vn.documentfileeditor.Model.Entity.CellStyle;
import hcmute.edu.vn.documentfileeditor.R;

public class SpreadsheetAdapter extends RecyclerView.Adapter<SpreadsheetAdapter.CellViewHolder> {
    public interface OnCellClickListener {
        void onCellClick(String cellKey);
    }

    private final String[] columns;
    private final int rows;
    private final Map<String, CellData> sheetData;
    private final OnCellClickListener cellClickListener;
    private final float density;

    public SpreadsheetAdapter(String[] columns, int rows, Map<String, CellData> sheetData,
                              float density, OnCellClickListener cellClickListener) {
        this.columns = columns;
        this.rows = rows;
        this.sheetData = sheetData;
        this.density = density;
        this.cellClickListener = cellClickListener;
    }

    @NonNull
    @Override
    public CellViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView textView = new TextView(parent.getContext());
        int heightPx = (int) (40 * density);
        textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, heightPx));
        textView.setPadding(16, 8, 16, 8);
        textView.setBackgroundResource(R.drawable.cell_border);
        textView.setGravity(Gravity.CENTER_VERTICAL);
        textView.setTextSize(14);
        return new CellViewHolder(textView);
    }

    @Override
    public void onBindViewHolder(@NonNull CellViewHolder holder, int position) {
        int row = position / columns.length;
        int col = position % columns.length;
        TextView textView = (TextView) holder.itemView;

        int widthDp = (col == 0) ? 50 : 100;
        int widthPx = (int) (widthDp * density);
        ViewGroup.LayoutParams params = textView.getLayoutParams();
        params.width = widthPx;
        textView.setLayoutParams(params);

        if (row == 0) {
            textView.setText(columns[col]);
            textView.setBackgroundColor(0xFFE5E7EB);
            textView.setTypeface(null, android.graphics.Typeface.BOLD);
        } else if (col == 0) {
            textView.setText(String.valueOf(row));
            textView.setBackgroundColor(0xFFE5E7EB);
            textView.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            String cellKey = columns[col] + row;
            CellData cellData = sheetData.get(cellKey);
            String displayText = cellData != null ? cellData.getValue() : "";

            textView.setText(displayText);
            textView.setBackgroundColor(android.graphics.Color.parseColor(
                    cellData != null && cellData.getStyle() != null ? cellData.getStyle().getBgColor() : "#FFFFFF"));

            int typeface = android.graphics.Typeface.NORMAL;
            if (cellData != null && cellData.getStyle() != null) {
                CellStyle style = cellData.getStyle();
                if (style.isBold()) {
                    typeface |= android.graphics.Typeface.BOLD;
                }
                if (style.isItalic()) {
                    typeface |= android.graphics.Typeface.ITALIC;
                }
            }
            textView.setTypeface(null, typeface);

            int gravity = Gravity.CENTER_VERTICAL;
            if (cellData != null && cellData.getStyle() != null) {
                switch (cellData.getStyle().getAlign().toLowerCase(Locale.US)) {
                    case "center":
                        gravity |= Gravity.CENTER_HORIZONTAL;
                        break;
                    case "right":
                        gravity |= Gravity.END;
                        break;
                    default:
                        gravity |= Gravity.START;
                        break;
                }
            } else {
                gravity |= Gravity.START;
            }
            textView.setGravity(gravity);

            textView.setOnClickListener(v -> {
                if (cellClickListener != null) {
                    cellClickListener.onCellClick(cellKey);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return rows * columns.length;
    }

    static class CellViewHolder extends RecyclerView.ViewHolder {
        CellViewHolder(View itemView) {
            super(itemView);
        }
    }
}
