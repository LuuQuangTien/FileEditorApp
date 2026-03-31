import { useState, useRef } from "react";
import { useNavigate } from "react-router";
import {
  Camera,
  Upload,
  RotateCw,
  Crop,
  Zap,
  Download,
  Check,
  X,
  ArrowLeft,
  Grid3x3,
  Sparkles,
} from "lucide-react";
import { Button } from "../components/ui/button";
import { cn } from "../components/ui/utils";
import { toast } from "sonner";

export function Scan() {
  const navigate = useNavigate();
  const [scannedImage, setScannedImage] = useState<string | null>(null);
  const [isEnhancing, setIsEnhancing] = useState(false);
  const [isProcessed, setIsProcessed] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        setScannedImage(event.target?.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleEnhance = () => {
    setIsEnhancing(true);
    // Simulate AI enhancement with Gemini
    setTimeout(() => {
      setIsEnhancing(false);
      setIsProcessed(true);
      toast.success("Image enhanced with AI!");
    }, 2000);
  };

  const handleSave = () => {
    toast.success("Document saved to library!");
    navigate("/files");
  };

  const handleOCR = () => {
    navigate("/ocr");
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
          <h1 className="text-xl">Document Scanner</h1>
        </div>
        <p className="text-sm text-neutral-600">
          Scan documents like a professional scanner
        </p>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto">
        {!scannedImage ? (
          <div className="p-4 space-y-4">
            {/* Camera/Upload Options */}
            <div className="space-y-3">
              <button
                onClick={() => fileInputRef.current?.click()}
                className="w-full bg-white border-2 border-dashed border-neutral-300 rounded-2xl p-8 hover:border-blue-400 hover:bg-blue-50 transition-all"
              >
                <div className="flex flex-col items-center gap-4">
                  <div className="p-4 bg-blue-100 rounded-full">
                    <Camera className="size-8 text-blue-600" />
                  </div>
                  <div>
                    <p className="font-medium mb-1">Take Photo</p>
                    <p className="text-sm text-neutral-500">
                      Use camera to scan document
                    </p>
                  </div>
                </div>
              </button>

              <button
                onClick={() => fileInputRef.current?.click()}
                className="w-full bg-white border border-neutral-200 rounded-2xl p-6 hover:border-blue-300 hover:shadow-sm transition-all"
              >
                <div className="flex items-center gap-4">
                  <div className="p-3 bg-purple-100 rounded-xl">
                    <Upload className="size-6 text-purple-600" />
                  </div>
                  <div className="text-left">
                    <p className="font-medium">Upload from Gallery</p>
                    <p className="text-sm text-neutral-500">
                      Choose from your photos
                    </p>
                  </div>
                </div>
              </button>

              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                capture="environment"
                className="hidden"
                onChange={handleFileUpload}
              />
            </div>

            {/* Features */}
            <div className="bg-white border border-neutral-200 rounded-2xl p-4">
              <h3 className="font-semibold mb-4">Features</h3>
              <div className="space-y-4">
                <div className="flex items-start gap-3">
                  <div className="p-2 bg-blue-100 rounded-lg shrink-0">
                    <Sparkles className="size-4 text-blue-600" />
                  </div>
                  <div>
                    <p className="font-medium text-sm">AI Enhancement</p>
                    <p className="text-xs text-neutral-600">
                      Automatically enhance clarity and remove shadows
                    </p>
                  </div>
                </div>
                <div className="flex items-start gap-3">
                  <div className="p-2 bg-green-100 rounded-lg shrink-0">
                    <Grid3x3 className="size-4 text-green-600" />
                  </div>
                  <div>
                    <p className="font-medium text-sm">Auto Crop</p>
                    <p className="text-xs text-neutral-600">
                      Detects document edges and crops automatically
                    </p>
                  </div>
                </div>
                <div className="flex items-start gap-3">
                  <div className="p-2 bg-purple-100 rounded-lg shrink-0">
                    <Zap className="size-4 text-purple-600" />
                  </div>
                  <div>
                    <p className="font-medium text-sm">Fast Processing</p>
                    <p className="text-xs text-neutral-600">
                      Powered by Google Gemini AI
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {/* Tips */}
            <div className="bg-gradient-to-br from-blue-50 to-purple-50 border border-blue-200 rounded-2xl p-4">
              <h3 className="font-semibold mb-2 text-sm">📸 Tips for best results</h3>
              <ul className="text-sm text-neutral-700 space-y-1">
                <li>• Place document on flat surface</li>
                <li>• Ensure good lighting</li>
                <li>• Keep camera parallel to document</li>
                <li>• Avoid shadows on the document</li>
              </ul>
            </div>
          </div>
        ) : (
          <div className="p-4 space-y-4">
            {/* Scanned Image Preview */}
            <div className="bg-white border border-neutral-200 rounded-2xl p-4 relative">
              {isEnhancing && (
                <div className="absolute inset-0 bg-white/90 flex items-center justify-center rounded-2xl z-10">
                  <div className="text-center">
                    <div className="inline-block p-3 bg-blue-100 rounded-full mb-3">
                      <Sparkles className="size-6 text-blue-600 animate-pulse" />
                    </div>
                    <p className="font-medium">Enhancing with AI...</p>
                    <p className="text-sm text-neutral-600">Using Gemini Vision</p>
                  </div>
                </div>
              )}
              <img
                src={scannedImage}
                alt="Scanned document"
                className="w-full h-auto rounded-lg"
              />
              {isProcessed && (
                <div className="absolute top-6 right-6 bg-green-500 text-white px-3 py-1 rounded-full text-sm flex items-center gap-1">
                  <Check className="size-4" />
                  Enhanced
                </div>
              )}
            </div>

            {/* Action Buttons */}
            <div className="grid grid-cols-3 gap-2">
              <button className="bg-white border border-neutral-200 rounded-xl p-3 flex flex-col items-center gap-2 hover:border-blue-300 transition-colors">
                <RotateCw className="size-5 text-neutral-700" />
                <span className="text-xs">Rotate</span>
              </button>
              <button className="bg-white border border-neutral-200 rounded-xl p-3 flex flex-col items-center gap-2 hover:border-blue-300 transition-colors">
                <Crop className="size-5 text-neutral-700" />
                <span className="text-xs">Crop</span>
              </button>
              <button
                onClick={handleEnhance}
                disabled={isEnhancing || isProcessed}
                className="bg-white border border-neutral-200 rounded-xl p-3 flex flex-col items-center gap-2 hover:border-blue-300 transition-colors disabled:opacity-50"
              >
                <Zap className="size-5 text-blue-600" />
                <span className="text-xs">Enhance</span>
              </button>
            </div>

            {/* Save Options */}
            <div className="space-y-2">
              <Button
                onClick={handleSave}
                className="w-full h-12 gap-2"
              >
                <Check className="size-5" />
                Save to Library
              </Button>
              <Button
                onClick={handleOCR}
                variant="outline"
                className="w-full h-12 gap-2"
              >
                <Sparkles className="size-5" />
                Extract Text (OCR)
              </Button>
              <Button
                variant="outline"
                className="w-full h-12 gap-2"
              >
                <Download className="size-5" />
                Download
              </Button>
            </div>

            {/* Retake */}
            <Button
              variant="ghost"
              onClick={() => {
                setScannedImage(null);
                setIsProcessed(false);
              }}
              className="w-full gap-2 text-neutral-600"
            >
              <X className="size-4" />
              Retake Photo
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
