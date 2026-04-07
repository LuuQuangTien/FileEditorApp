import { useState } from "react";
import { useNavigate } from "react-router";
import { Upload, FileText, Image as ImageIcon, Copy, Download, Loader2, Menu, Sparkles, Languages, ArrowLeft } from "lucide-react";
import { Button } from "../components/ui/button";
import { Textarea } from "../components/ui/textarea";
import { cn } from "../components/ui/utils";
import { toast } from "sonner";

export function OCR() {
  const navigate = useNavigate();
  const [isDragging, setIsDragging] = useState(false);
  const [uploadedFile, setUploadedFile] = useState<File | null>(null);
  const [extractedText, setExtractedText] = useState("");
  const [isProcessing, setIsProcessing] = useState(false);

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    const file = e.dataTransfer.files[0];
    if (file) {
      handleFileUpload(file);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      handleFileUpload(file);
    }
  };

  const handleFileUpload = (file: File) => {
    setUploadedFile(file);
    simulateOCR();
  };

  const simulateOCR = () => {
    setIsProcessing(true);
    setExtractedText("");
    
    // Simulate Gemini API OCR processing
    setTimeout(() => {
      setExtractedText(
        `[Extracted with Google Gemini Vision API]\n\nThis is a demonstration of OCR (Optical Character Recognition) functionality powered by Google Gemini AI. In a production environment, this would extract actual text from images and PDF files with high accuracy.\n\nKey features:\n• Advanced AI text recognition\n• Multi-language support (100+ languages)\n• Handwriting recognition\n• Table and layout detection\n• High accuracy (99%+)\n\nThe extracted text can be:\n- Copied to clipboard\n- Downloaded as a text file\n- Translated to other languages\n- Edited and exported\n- Used for further AI processing\n\nThis OCR system uses Google Gemini Vision API to recognize text in various fonts, sizes, and styles with exceptional accuracy, even in challenging conditions like poor lighting or rotated images.`
      );
      setIsProcessing(false);
      toast.success("Text extracted with Gemini AI!");
    }, 2000);
  };

  const handleCopyText = () => {
    navigator.clipboard.writeText(extractedText);
    toast.success("Text copied to clipboard!");
  };

  const handleDownloadText = () => {
    const blob = new Blob([extractedText], { type: "text/plain" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "extracted-text.txt";
    a.click();
    toast.success("Text file downloaded!");
  };

  const handleTranslate = () => {
    navigate("/translate");
  };

  return (
    <div className="h-full flex flex-col bg-neutral-50">
      {/* Header */}
      <div className="bg-white border-b border-neutral-200 px-4 py-3">
        <div className="flex items-center gap-3 mb-2">
          <button
            onClick={() => navigate("/")}
            className="p-2 hover:bg-neutral-100 rounded-lg"
          >
            <ArrowLeft className="size-5" />
          </button>
          <h1 className="text-xl">OCR Scanner</h1>
        </div>
        <div className="flex items-center gap-2 text-sm text-neutral-600">
          <Sparkles className="size-4" />
          <p>Extract text with Google Gemini AI</p>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto">
        <div className="p-4 space-y-4">
          {/* Upload Area */}
          <div
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            className={cn(
              "border-2 border-dashed rounded-xl p-8 text-center transition-colors bg-white",
              isDragging
                ? "border-blue-500 bg-blue-50"
                : "border-neutral-300"
            )}
          >
            <div className="flex flex-col items-center gap-4">
              <div className="p-4 bg-blue-50 rounded-full">
                <Upload className="size-8 text-blue-600" />
              </div>
              <div>
                <p className="font-medium mb-1">
                  Tap to upload a file
                </p>
                <p className="text-sm text-neutral-500 mb-4">
                  JPG, PNG, PDF (Max 10MB)
                </p>
                <input
                  type="file"
                  id="file-upload"
                  className="hidden"
                  accept="image/*,.pdf"
                  onChange={handleFileSelect}
                />
                <label htmlFor="file-upload">
                  <Button className="h-11 px-6 cursor-pointer">
                    Select File
                  </Button>
                </label>
              </div>
            </div>
          </div>

          {/* Uploaded File Info */}
          {uploadedFile && (
            <div className="bg-white border border-neutral-200 rounded-xl p-4">
              <div className="flex items-center gap-3">
                <div className="p-3 bg-blue-50 rounded-lg shrink-0">
                  {uploadedFile.type.startsWith("image/") ? (
                    <ImageIcon className="size-5 text-blue-600" />
                  ) : (
                    <FileText className="size-5 text-red-600" />
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium truncate">{uploadedFile.name}</p>
                  <p className="text-sm text-neutral-500">
                    {(uploadedFile.size / 1024).toFixed(2)} KB
                  </p>
                </div>
                {isProcessing && (
                  <div className="shrink-0 flex items-center gap-2">
                    <Loader2 className="size-5 text-blue-600 animate-spin" />
                    <span className="text-sm text-neutral-600">Processing...</span>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Extracted Text */}
          {(extractedText || isProcessing) && (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <h2 className="font-semibold">Extracted Text</h2>
                {extractedText && (
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={handleCopyText}
                      className="gap-2"
                    >
                      <Copy className="size-4" />
                      Copy
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={handleTranslate}
                      className="gap-2"
                    >
                      <Languages className="size-4" />
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={handleDownloadText}
                      className="gap-2"
                    >
                      <Download className="size-4" />
                    </Button>
                  </div>
                )}
              </div>

              <div className="bg-white border border-neutral-200 rounded-xl min-h-[400px] flex flex-col">
                {isProcessing ? (
                  <div className="flex-1 flex flex-col items-center justify-center gap-4 p-8">
                    <div className="p-3 bg-blue-100 rounded-full">
                      <Sparkles className="size-8 text-blue-600 animate-pulse" />
                    </div>
                    <div className="text-center">
                      <p className="font-medium mb-1">Processing with Gemini AI...</p>
                      <p className="text-sm text-neutral-600">Extracting text from your document</p>
                    </div>
                  </div>
                ) : (
                  <Textarea
                    value={extractedText}
                    onChange={(e) => setExtractedText(e.target.value)}
                    className="flex-1 min-h-[400px] resize-none border-0 focus-visible:ring-0 rounded-xl"
                    placeholder="Extracted text will appear here..."
                  />
                )}
              </div>
            </div>
          )}

          {/* Features - Only show when no file uploaded */}
          {!uploadedFile && (
            <div className="bg-white border border-neutral-200 rounded-xl p-4">
              <h3 className="font-semibold mb-4">Powered by Google Gemini AI</h3>
              <div className="space-y-4">
                <div className="flex items-start gap-3">
                  <div className="size-2 bg-blue-600 rounded-full mt-2 shrink-0"></div>
                  <div>
                    <p className="font-medium">99%+ Accuracy</p>
                    <p className="text-sm text-neutral-600">
                      Advanced Gemini Vision API for precise text recognition
                    </p>
                  </div>
                </div>
                <div className="flex items-start gap-3">
                  <div className="size-2 bg-blue-600 rounded-full mt-2 shrink-0"></div>
                  <div>
                    <p className="font-medium">Multi-Language Support</p>
                    <p className="text-sm text-neutral-600">
                      Supports 100+ languages including Vietnamese, Chinese, Japanese
                    </p>
                  </div>
                </div>
                <div className="flex items-start gap-3">
                  <div className="size-2 bg-blue-600 rounded-full mt-2 shrink-0"></div>
                  <div>
                    <p className="font-medium">Smart Detection</p>
                    <p className="text-sm text-neutral-600">
                      Recognizes tables, layouts, and handwriting
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}