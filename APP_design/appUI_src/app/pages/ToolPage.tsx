import { useState, useRef } from "react";
import { useNavigate, useParams } from "react-router";
import {
  ArrowLeft,
  Upload,
  Download,
  FileText,
  Sparkles,
  Loader2,
  Check,
  Image as ImageIcon,
} from "lucide-react";
import { Button } from "../components/ui/button";
import { cn } from "../components/ui/utils";
import { toast } from "sonner";

type ToolConfig = {
  id: string;
  title: string;
  description: string;
  acceptedFormats: string;
  outputFormat: string;
  icon: React.ComponentType<{ className?: string }>;
  successMessage: string;
};

const toolConfigs: Record<string, ToolConfig> = {
  "pdf-to-word": {
    id: "pdf-to-word",
    title: "PDF to Word",
    description: "Convert your PDF files to editable Word documents",
    acceptedFormats: ".pdf",
    outputFormat: "DOCX",
    icon: FileText,
    successMessage: "PDF converted to Word successfully!",
  },
  "pdf-to-excel": {
    id: "pdf-to-excel",
    title: "PDF to Excel",
    description: "Extract tables from PDF to Excel spreadsheets",
    acceptedFormats: ".pdf",
    outputFormat: "XLSX",
    icon: FileText,
    successMessage: "PDF converted to Excel successfully!",
  },
  "image-to-pdf": {
    id: "image-to-pdf",
    title: "Image to PDF",
    description: "Convert images to PDF documents",
    acceptedFormats: "image/*",
    outputFormat: "PDF",
    icon: ImageIcon,
    successMessage: "Images converted to PDF successfully!",
  },
  "merge-pdf": {
    id: "merge-pdf",
    title: "Merge PDF",
    description: "Combine multiple PDF files into one",
    acceptedFormats: ".pdf",
    outputFormat: "PDF",
    icon: FileText,
    successMessage: "PDFs merged successfully!",
  },
  "split-pdf": {
    id: "split-pdf",
    title: "Split PDF",
    description: "Split PDF into individual pages or ranges",
    acceptedFormats: ".pdf",
    outputFormat: "PDF",
    icon: FileText,
    successMessage: "PDF split successfully!",
  },
  "crop-image": {
    id: "crop-image",
    title: "Crop Image",
    description: "Crop and resize your images",
    acceptedFormats: "image/*",
    outputFormat: "Image",
    icon: ImageIcon,
    successMessage: "Image cropped successfully!",
  },
  "rotate-image": {
    id: "rotate-image",
    title: "Rotate Image",
    description: "Rotate and flip your images",
    acceptedFormats: "image/*",
    outputFormat: "Image",
    icon: ImageIcon,
    successMessage: "Image rotated successfully!",
  },
  "enhance-image": {
    id: "enhance-image",
    title: "Enhance Image",
    description: "Improve image quality with AI",
    acceptedFormats: "image/*",
    outputFormat: "Image",
    icon: Sparkles,
    successMessage: "Image enhanced with AI!",
  },
  "annotate-pdf": {
    id: "annotate-pdf",
    title: "Annotate PDF",
    description: "Add highlights and notes to your PDF",
    acceptedFormats: ".pdf",
    outputFormat: "PDF",
    icon: FileText,
    successMessage: "PDF annotated successfully!",
  },
};

export function ToolPage() {
  const navigate = useNavigate();
  const { toolId } = useParams<{ toolId: string }>();
  const [uploadedFiles, setUploadedFiles] = useState<File[]>([]);
  const [isProcessing, setIsProcessing] = useState(false);
  const [isComplete, setIsComplete] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const config = toolId ? toolConfigs[toolId] : null;

  if (!config) {
    return (
      <div className="h-full flex items-center justify-center bg-neutral-50">
        <div className="text-center">
          <p className="text-lg font-medium mb-2">Tool not found</p>
          <Button onClick={() => navigate("/tools")}>Back to Tools</Button>
        </div>
      </div>
    );
  }

  const Icon = config.icon;

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);
    if (files.length > 0) {
      setUploadedFiles((prev) => [...prev, ...files]);
    }
  };

  const handleProcess = () => {
    if (uploadedFiles.length === 0) {
      toast.error("Please upload at least one file");
      return;
    }

    setIsProcessing(true);

    // Simulate processing with Gemini API
    setTimeout(() => {
      setIsProcessing(false);
      setIsComplete(true);
      toast.success(config.successMessage);
    }, 2500);
  };

  const handleDownload = () => {
    toast.success(`${config.outputFormat} file downloaded!`);
  };

  const handleRemoveFile = (index: number) => {
    setUploadedFiles((prev) => prev.filter((_, i) => i !== index));
  };

  return (
    <div className="h-full flex flex-col bg-neutral-50">
      {/* Header */}
      <div className="bg-white border-b border-neutral-200 px-4 py-3">
        <div className="flex items-center gap-3 mb-2">
          <button
            onClick={() => navigate("/tools")}
            className="p-2 hover:bg-neutral-100 rounded-lg"
          >
            <ArrowLeft className="size-5" />
          </button>
          <h1 className="text-xl">{config.title}</h1>
        </div>
        <p className="text-sm text-neutral-600">{config.description}</p>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto p-4 space-y-4">
        {/* Upload Area */}
        {!isComplete && (
          <div
            onClick={() => fileInputRef.current?.click()}
            className="border-2 border-dashed border-neutral-300 rounded-2xl p-8 text-center bg-white hover:border-blue-400 hover:bg-blue-50 transition-all cursor-pointer"
          >
            <div className="flex flex-col items-center gap-4">
              <div className="p-4 bg-blue-100 rounded-full">
                <Upload className="size-8 text-blue-600" />
              </div>
              <div>
                <p className="font-medium mb-1">Upload Files</p>
                <p className="text-sm text-neutral-500">
                  Tap to select {config.acceptedFormats === "image/*" ? "images" : "files"}
                </p>
              </div>
              <input
                ref={fileInputRef}
                type="file"
                accept={config.acceptedFormats}
                multiple={config.id === "merge-pdf" || config.id === "image-to-pdf"}
                className="hidden"
                onChange={handleFileUpload}
              />
            </div>
          </div>
        )}

        {/* Uploaded Files */}
        {uploadedFiles.length > 0 && !isComplete && (
          <div className="space-y-2">
            <h3 className="font-semibold text-sm">
              Uploaded Files ({uploadedFiles.length})
            </h3>
            <div className="space-y-2">
              {uploadedFiles.map((file, index) => (
                <div
                  key={index}
                  className="bg-white border border-neutral-200 rounded-xl p-3 flex items-center gap-3"
                >
                  <div className="p-2 bg-blue-100 rounded-lg">
                    <Icon className="size-5 text-blue-600" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="font-medium text-sm truncate">{file.name}</p>
                    <p className="text-xs text-neutral-500">
                      {(file.size / 1024).toFixed(2)} KB
                    </p>
                  </div>
                  <button
                    onClick={() => handleRemoveFile(index)}
                    className="text-red-600 text-sm hover:bg-red-50 px-2 py-1 rounded"
                  >
                    Remove
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Processing */}
        {isProcessing && (
          <div className="bg-white border border-neutral-200 rounded-2xl p-8">
            <div className="flex flex-col items-center gap-4 text-center">
              <div className="p-4 bg-blue-100 rounded-full">
                <Sparkles className="size-8 text-blue-600 animate-pulse" />
              </div>
              <div>
                <p className="font-medium mb-1">Processing...</p>
                <p className="text-sm text-neutral-600">
                  Using AI to convert your files
                </p>
              </div>
              <Loader2 className="size-6 text-blue-600 animate-spin" />
            </div>
          </div>
        )}

        {/* Complete */}
        {isComplete && (
          <div className="space-y-4">
            <div className="bg-white border border-green-200 rounded-2xl p-8">
              <div className="flex flex-col items-center gap-4 text-center">
                <div className="p-4 bg-green-100 rounded-full">
                  <Check className="size-8 text-green-600" />
                </div>
                <div>
                  <p className="font-medium mb-1">Conversion Complete!</p>
                  <p className="text-sm text-neutral-600">
                    Your {config.outputFormat} file is ready
                  </p>
                </div>
              </div>
            </div>

            <div className="space-y-2">
              <Button onClick={handleDownload} className="w-full h-12 gap-2">
                <Download className="size-5" />
                Download {config.outputFormat}
              </Button>
              <Button
                variant="outline"
                onClick={() => {
                  setUploadedFiles([]);
                  setIsComplete(false);
                }}
                className="w-full h-12"
              >
                Convert Another File
              </Button>
            </div>
          </div>
        )}

        {/* Process Button */}
        {uploadedFiles.length > 0 && !isProcessing && !isComplete && (
          <Button onClick={handleProcess} className="w-full h-12 gap-2">
            <Sparkles className="size-5" />
            Convert to {config.outputFormat}
          </Button>
        )}

        {/* Info */}
        {!isComplete && (
          <div className="bg-gradient-to-br from-blue-50 to-purple-50 border border-blue-200 rounded-2xl p-4">
            <h3 className="font-semibold mb-2 text-sm flex items-center gap-2">
              <Sparkles className="size-4 text-purple-600" />
              AI-Powered Conversion
            </h3>
            <ul className="text-sm text-neutral-700 space-y-1">
              <li>• High-quality output with AI enhancement</li>
              <li>• Fast processing powered by Google Gemini</li>
              <li>• Preserves formatting and layout</li>
              <li>• Secure and private processing</li>
            </ul>
          </div>
        )}
      </div>
    </div>
  );
}
