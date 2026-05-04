package hcmute.edu.vn.documentfileeditor.Model.Entity;

public class CellData {
    private String value;
    private String formula;
    private CellStyle style;

    public CellData(String value) {
        this.value = value;
        this.style = new CellStyle();
    }

    public CellData(String value, String formula) {
        this.value = value;
        this.formula = formula;
        this.style = new CellStyle();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public CellStyle getStyle() {
        return style;
    }

    public void setStyle(CellStyle style) {
        this.style = style;
    }
}
