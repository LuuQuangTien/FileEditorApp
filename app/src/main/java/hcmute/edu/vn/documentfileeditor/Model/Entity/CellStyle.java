package hcmute.edu.vn.documentfileeditor.Model.Entity;

public class CellStyle {
    private boolean bold;
    private boolean italic;
    private boolean underline;
    private String align;
    private String bgColor;
    private String textColor;
    private String fontSize;

    public CellStyle() {
        this.bold = false;
        this.italic = false;
        this.underline = false;
        this.align = "left";
        this.bgColor = "#FFFFFF";
        this.textColor = "#000000";
        this.fontSize = "14";
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public boolean isUnderline() {
        return underline;
    }

    public void setUnderline(boolean underline) {
        this.underline = underline;
    }

    public String getAlign() {
        return align;
    }

    public void setAlign(String align) {
        this.align = align;
    }

    public String getBgColor() {
        return bgColor;
    }

    public void setBgColor(String bgColor) {
        this.bgColor = bgColor;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public String getFontSize() {
        return fontSize;
    }

    public void setFontSize(String fontSize) {
        this.fontSize = fontSize;
    }
}
