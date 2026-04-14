import { useState } from "react";
import { useNavigate, useParams } from "react-router";
import {
  ArrowLeft,
  Save,
  Share2,
  MoreVertical,
  Plus,
  Trash2,
  Download,
  FileSpreadsheet,
  Type,
  Palette,
  AlignLeft,
  AlignCenter,
  AlignRight,
  Bold,
  Italic,
  Underline,
  Table,
} from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { cn } from "../components/ui/utils";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "../components/ui/dropdown-menu";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../components/ui/select";
import { toast } from "sonner";

type CellData = {
  value: string;
  formula?: string;
  style?: {
    bold?: boolean;
    italic?: boolean;
    underline?: boolean;
    align?: "left" | "center" | "right";
    bgColor?: string;
    textColor?: string;
    fontSize?: string;
  };
};

type SheetData = {
  [key: string]: CellData;
};

const COLUMNS = ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J"];
const ROWS = Array.from({ length: 20 }, (_, i) => i + 1);

export function ExcelEditor() {
  const navigate = useNavigate();
  const { id } = useParams();
  const [sheetData, setSheetData] = useState<SheetData>({
    A1: { value: "Product" },
    B1: { value: "Quantity" },
    C1: { value: "Price" },
    D1: { value: "Total" },
    A2: { value: "Laptop" },
    B2: { value: "5" },
    C2: { value: "1200" },
    D2: { value: "6000", formula: "=B2*C2" },
    A3: { value: "Mouse" },
    B3: { value: "15" },
    C3: { value: "25" },
    D3: { value: "375", formula: "=B3*C3" },
    A4: { value: "Keyboard" },
    B4: { value: "10" },
    C4: { value: "75" },
    D4: { value: "750", formula: "=B4*C4" },
  });
  const [selectedCell, setSelectedCell] = useState<string | null>(null);
  const [editingCell, setEditingCell] = useState<string | null>(null);
  const [formulaBar, setFormulaBar] = useState("");
  const [sheetName, setSheetName] = useState("Sheet1");

  const getCellKey = (col: string, row: number) => `${col}${row}`;

  const getCellData = (cellKey: string): CellData => {
    return sheetData[cellKey] || { value: "" };
  };

  const updateCell = (cellKey: string, value: string) => {
    setSheetData({
      ...sheetData,
      [cellKey]: {
        ...getCellData(cellKey),
        value,
      },
    });
  };

  const handleCellClick = (cellKey: string) => {
    setSelectedCell(cellKey);
    const cellData = getCellData(cellKey);
    setFormulaBar(cellData.formula || cellData.value);
  };

  const handleCellDoubleClick = (cellKey: string) => {
    setEditingCell(cellKey);
    const cellData = getCellData(cellKey);
    setFormulaBar(cellData.formula || cellData.value);
  };

  const handleCellEdit = (cellKey: string, value: string) => {
    updateCell(cellKey, value);
    setFormulaBar(value);
  };

  const handleFormulaBarChange = (value: string) => {
    setFormulaBar(value);
    if (selectedCell) {
      updateCell(selectedCell, value);
    }
  };

  const applyCellStyle = (styleUpdate: Partial<CellData["style"]>) => {
    if (!selectedCell) return;
    const cellData = getCellData(selectedCell);
    setSheetData({
      ...sheetData,
      [selectedCell]: {
        ...cellData,
        style: {
          ...cellData.style,
          ...styleUpdate,
        },
      },
    });
  };

  const handleSave = () => {
    toast.success("Spreadsheet saved!");
  };

  const exportToCSV = () => {
    toast.success("Exported as CSV!");
  };

  return (
    <div className="h-full flex flex-col bg-white">
      {/* Header */}
      <div className="border-b border-neutral-200 px-4 py-3">
        <div className="flex items-center justify-between mb-3">
          <button
            onClick={() => navigate(-1)}
            className="p-2 hover:bg-neutral-100 rounded-lg active:scale-95 transition-transform"
          >
            <ArrowLeft className="size-5" />
          </button>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={handleSave}
              className="gap-2"
            >
              <Save className="size-4" />
              Save
            </Button>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="outline" size="sm">
                  <MoreVertical className="size-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem onClick={exportToCSV}>
                  <Download className="size-4 mr-2" />
                  Export as CSV
                </DropdownMenuItem>
                <DropdownMenuItem>
                  <FileSpreadsheet className="size-4 mr-2" />
                  Export as XLSX
                </DropdownMenuItem>
                <DropdownMenuItem>
                  <Share2 className="size-4 mr-2" />
                  Share
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
        <h1 className="text-lg font-semibold truncate">
          Sales Report.xlsx
        </h1>
      </div>

      {/* Toolbar */}
      <div className="border-b border-neutral-200 px-4 py-2 overflow-x-auto">
        <div className="flex items-center gap-2 min-w-max">
          {/* Font Size */}
          <Select defaultValue="14">
            <SelectTrigger className="w-20 h-8">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="10">10</SelectItem>
              <SelectItem value="12">12</SelectItem>
              <SelectItem value="14">14</SelectItem>
              <SelectItem value="16">16</SelectItem>
              <SelectItem value="18">18</SelectItem>
              <SelectItem value="20">20</SelectItem>
            </SelectContent>
          </Select>

          <div className="w-px h-6 bg-neutral-200"></div>

          {/* Text Formatting */}
          <button
            onClick={() =>
              applyCellStyle({
                bold: !getCellData(selectedCell || "").style?.bold,
              })
            }
            className={cn(
              "p-2 rounded-lg transition-colors",
              getCellData(selectedCell || "").style?.bold
                ? "bg-blue-100 text-blue-600"
                : "hover:bg-neutral-100"
            )}
            disabled={!selectedCell}
          >
            <Bold className="size-4" />
          </button>
          <button
            onClick={() =>
              applyCellStyle({
                italic: !getCellData(selectedCell || "").style?.italic,
              })
            }
            className={cn(
              "p-2 rounded-lg transition-colors",
              getCellData(selectedCell || "").style?.italic
                ? "bg-blue-100 text-blue-600"
                : "hover:bg-neutral-100"
            )}
            disabled={!selectedCell}
          >
            <Italic className="size-4" />
          </button>
          <button
            onClick={() =>
              applyCellStyle({
                underline: !getCellData(selectedCell || "").style?.underline,
              })
            }
            className={cn(
              "p-2 rounded-lg transition-colors",
              getCellData(selectedCell || "").style?.underline
                ? "bg-blue-100 text-blue-600"
                : "hover:bg-neutral-100"
            )}
            disabled={!selectedCell}
          >
            <Underline className="size-4" />
          </button>

          <div className="w-px h-6 bg-neutral-200"></div>

          {/* Alignment */}
          <button
            onClick={() => applyCellStyle({ align: "left" })}
            className={cn(
              "p-2 rounded-lg transition-colors",
              getCellData(selectedCell || "").style?.align === "left"
                ? "bg-blue-100 text-blue-600"
                : "hover:bg-neutral-100"
            )}
            disabled={!selectedCell}
          >
            <AlignLeft className="size-4" />
          </button>
          <button
            onClick={() => applyCellStyle({ align: "center" })}
            className={cn(
              "p-2 rounded-lg transition-colors",
              getCellData(selectedCell || "").style?.align === "center"
                ? "bg-blue-100 text-blue-600"
                : "hover:bg-neutral-100"
            )}
            disabled={!selectedCell}
          >
            <AlignCenter className="size-4" />
          </button>
          <button
            onClick={() => applyCellStyle({ align: "right" })}
            className={cn(
              "p-2 rounded-lg transition-colors",
              getCellData(selectedCell || "").style?.align === "right"
                ? "bg-blue-100 text-blue-600"
                : "hover:bg-neutral-100"
            )}
            disabled={!selectedCell}
          >
            <AlignRight className="size-4" />
          </button>

          <div className="w-px h-6 bg-neutral-200"></div>

          {/* Colors */}
          <button
            onClick={() => applyCellStyle({ bgColor: "#fef3c7" })}
            className="p-2 rounded-lg hover:bg-neutral-100"
            disabled={!selectedCell}
          >
            <div className="size-4 bg-yellow-200 border border-neutral-300 rounded"></div>
          </button>
          <button
            onClick={() => applyCellStyle({ bgColor: "#dbeafe" })}
            className="p-2 rounded-lg hover:bg-neutral-100"
            disabled={!selectedCell}
          >
            <div className="size-4 bg-blue-200 border border-neutral-300 rounded"></div>
          </button>
          <button
            onClick={() => applyCellStyle({ bgColor: "#dcfce7" })}
            className="p-2 rounded-lg hover:bg-neutral-100"
            disabled={!selectedCell}
          >
            <div className="size-4 bg-green-200 border border-neutral-300 rounded"></div>
          </button>
        </div>
      </div>

      {/* Formula Bar */}
      <div className="border-b border-neutral-200 px-4 py-2 flex items-center gap-2">
        <div className="text-sm font-semibold text-neutral-700 w-12">
          {selectedCell || ""}
        </div>
        <Input
          value={formulaBar}
          onChange={(e) => handleFormulaBarChange(e.target.value)}
          placeholder="Enter value or formula (e.g., =A1+B1)"
          className="flex-1 h-8"
          disabled={!selectedCell}
        />
      </div>

      {/* Spreadsheet Grid */}
      <div className="flex-1 overflow-auto">
        <div className="inline-block min-w-full">
          <table className="border-collapse">
            <thead>
              <tr>
                <th className="sticky top-0 left-0 z-20 bg-neutral-100 border border-neutral-300 w-12 h-8"></th>
                {COLUMNS.map((col) => (
                  <th
                    key={col}
                    className="sticky top-0 z-10 bg-neutral-100 border border-neutral-300 w-24 h-8 text-sm font-semibold"
                  >
                    {col}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {ROWS.map((row) => (
                <tr key={row}>
                  <td className="sticky left-0 z-10 bg-neutral-100 border border-neutral-300 text-center text-sm font-semibold">
                    {row}
                  </td>
                  {COLUMNS.map((col) => {
                    const cellKey = getCellKey(col, row);
                    const cellData = getCellData(cellKey);
                    const isSelected = selectedCell === cellKey;
                    const isEditing = editingCell === cellKey;

                    return (
                      <td
                        key={cellKey}
                        className={cn(
                          "border border-neutral-300 h-8 p-0 cursor-cell relative",
                          isSelected && "ring-2 ring-blue-500 ring-inset z-[5]"
                        )}
                        onClick={() => handleCellClick(cellKey)}
                        onDoubleClick={() => handleCellDoubleClick(cellKey)}
                        style={{
                          backgroundColor: cellData.style?.bgColor,
                        }}
                      >
                        {isEditing ? (
                          <input
                            type="text"
                            value={cellData.value}
                            onChange={(e) =>
                              handleCellEdit(cellKey, e.target.value)
                            }
                            onBlur={() => setEditingCell(null)}
                            onKeyDown={(e) => {
                              if (e.key === "Enter") {
                                setEditingCell(null);
                              }
                            }}
                            autoFocus
                            className="w-full h-full px-2 outline-none bg-transparent"
                          />
                        ) : (
                          <div
                            className={cn(
                              "px-2 h-8 flex items-center text-sm",
                              cellData.style?.bold && "font-bold",
                              cellData.style?.italic && "italic",
                              cellData.style?.underline && "underline",
                              cellData.style?.align === "center" &&
                                "justify-center",
                              cellData.style?.align === "right" &&
                                "justify-end"
                            )}
                            style={{
                              color: cellData.style?.textColor,
                            }}
                          >
                            {cellData.value}
                          </div>
                        )}
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Sheet Tabs */}
      <div className="border-t border-neutral-200 px-4 py-2 flex items-center gap-2 overflow-x-auto">
        <button className="px-4 py-2 bg-white border-t-2 border-blue-600 text-sm font-medium">
          {sheetName}
        </button>
        <button className="p-2 hover:bg-neutral-100 rounded-lg">
          <Plus className="size-4" />
        </button>
      </div>
    </div>
  );
}
