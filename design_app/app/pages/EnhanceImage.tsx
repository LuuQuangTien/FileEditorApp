import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router";
import {
  ArrowLeft,
  Download,
  Sparkles,
  Check,
  Wand2,
  Zap,
  Image as ImageIcon,
  Loader2,
} from "lucide-react";
import { Button } from "../components/ui/button";
import { cn } from "../components/ui/utils";
import { toast } from "sonner";

type Enhancement = {
  id: string;
  name: string;
  description: string;
  icon: React.ComponentType<{ className?: string }>;
};

export function EnhanceImage() {
  const navigate = useNavigate();
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [image, setImage] = useState<string | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [isEnhanced, setIsEnhanced] = useState(false);
  const [selectedEnhancement, setSelectedEnhancement] = useState<string | null>(null);

  const enhancements: Enhancement[] = [
    {
      id: "auto-enhance",
      name: "Auto Enhance",
      description: "AI-powered overall improvement",
      icon: Sparkles,
    },
    {
      id: "upscale",
      name: "Upscale Quality",
      description: "Increase resolution",
      icon: Zap,
    },
    {
      id: "denoise",
      name: "Reduce Noise",
      description: "Remove grain and artifacts",
      icon: Wand2,
    },
    {
      id: "sharpen",
      name: "Sharpen",
      description: "Enhance edges and details",
      icon: ImageIcon,
    },
  ];

  useEffect(() => {
    // Load a sample image
    const img = new Image();
    img.crossOrigin = "anonymous";
    img.src = "https://images.unsplash.com/photo-1682687220742-aba13b6e50ba?w=800&h=600&fit=crop";
    img.onload = () => {
      setImage(img.src);
      if (canvasRef.current) {
        const canvas = canvasRef.current;
        const ctx = canvas.getContext("2d");
        if (ctx) {
          canvas.width = img.width;
          canvas.height = img.height;
          ctx.drawImage(img, 0, 0);
        }
      }
    };
  }, []);

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        const img = new Image();
        img.onload = () => {
          setImage(event.target?.result as string);
          setIsEnhanced(false);
          setSelectedEnhancement(null);
          if (canvasRef.current) {
            const canvas = canvasRef.current;
            const ctx = canvas.getContext("2d");
            if (ctx) {
              canvas.width = img.width;
              canvas.height = img.height;
              ctx.drawImage(img, 0, 0);
            }
          }
        };
        img.src = event.target?.result as string;
      };
      reader.readAsDataURL(file);
    }
  };

  const applyEnhancement = (enhancementId: string) => {
    if (!canvasRef.current || !image) return;

    setSelectedEnhancement(enhancementId);
    setIsProcessing(true);

    // Simulate AI processing
    setTimeout(() => {
      const canvas = canvasRef.current;
      const ctx = canvas?.getContext("2d");
      
      if (ctx && canvas) {
        const img = new Image();
        img.src = image;
        img.onload = () => {
          // Apply different enhancements based on selection
          switch (enhancementId) {
            case "auto-enhance":
              // Simulate auto-enhance with brightness and contrast
              ctx.filter = "brightness(110%) contrast(110%) saturate(110%)";
              break;
            case "upscale":
              // Simulate upscaling by drawing larger
              canvas.width = img.width * 1.5;
              canvas.height = img.height * 1.5;
              ctx.imageSmoothingEnabled = true;
              ctx.imageSmoothingQuality = "high";
              break;
            case "denoise":
              // Simulate denoising with slight blur
              ctx.filter = "blur(0.5px) contrast(105%)";
              break;
            case "sharpen":
              // Simulate sharpening with contrast
              ctx.filter = "contrast(120%) brightness(105%)";
              break;
          }
          
          ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
          ctx.filter = "none";
          
          setIsProcessing(false);
          setIsEnhanced(true);
          toast.success("Image enhanced!");
        };
      }
    }, 2000);
  };

  const resetImage = () => {
    if (image && canvasRef.current) {
      const img = new Image();
      img.src = image;
      img.onload = () => {
        const canvas = canvasRef.current;
        const ctx = canvas?.getContext("2d");
        if (ctx && canvas) {
          canvas.width = img.width;
          canvas.height = img.height;
          ctx.filter = "none";
          ctx.drawImage(img, 0, 0);
          setIsEnhanced(false);
          setSelectedEnhancement(null);
          toast.success("Reset to original!");
        }
      };
    }
  };

  const downloadImage = () => {
    if (!canvasRef.current) return;
    const url = canvasRef.current.toDataURL("image/png");
    const link = document.createElement("a");
    link.download = "enhanced-image.png";
    link.href = url;
    link.click();
    toast.success("Image downloaded!");
  };

  return (
    <div className="h-full flex flex-col bg-neutral-50">
      {/* Header */}
      <div className="bg-white border-b border-neutral-200 px-4 py-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <button
              onClick={() => navigate(-1)}
              className="p-2 hover:bg-neutral-100 rounded-lg active:scale-95 transition-transform"
            >
              <ArrowLeft className="size-5" />
            </button>
            <div>
              <h1 className="text-lg font-semibold">Enhance Image</h1>
              <p className="text-xs text-neutral-600">AI-powered image enhancement</p>
            </div>
          </div>
          <div className="flex gap-2">
            {isEnhanced && (
              <Button
                variant="outline"
                size="sm"
                onClick={resetImage}
                className="gap-2"
              >
                Reset
              </Button>
            )}
            <Button
              variant="outline"
              size="sm"
              onClick={downloadImage}
              className="gap-2"
              disabled={!isEnhanced}
            >
              <Download className="size-4" />
            </Button>
          </div>
        </div>
      </div>

      {/* Canvas Area */}
      <div className="flex-1 overflow-auto p-4 pb-20">
        <div className="max-w-2xl mx-auto">
          {!image ? (
            <div className="bg-white rounded-xl border-2 border-dashed border-neutral-300 p-8 text-center">
              <div className="mb-4">
                <Sparkles className="size-12 mx-auto text-neutral-400" />
              </div>
              <h3 className="font-semibold mb-2">Upload an image to enhance</h3>
              <p className="text-sm text-neutral-600 mb-4">
                PNG, JPG up to 10MB
              </p>
              <label>
                <input
                  type="file"
                  accept="image/*"
                  onChange={handleFileUpload}
                  className="hidden"
                />
                <Button className="cursor-pointer">Select Image</Button>
              </label>
            </div>
          ) : (
            <div className="space-y-4">
              {/* Canvas Preview */}
              <div className="bg-white rounded-xl p-4 shadow-sm">
                <div className="flex justify-center items-center bg-neutral-50 rounded-lg overflow-hidden relative">
                  <canvas
                    ref={canvasRef}
                    className="max-w-full h-auto"
                  />
                  
                  {/* Processing Overlay */}
                  {isProcessing && (
                    <div className="absolute inset-0 bg-black/50 flex items-center justify-center rounded-lg">
                      <div className="bg-white rounded-xl p-6 flex flex-col items-center gap-3">
                        <Loader2 className="size-8 text-blue-600 animate-spin" />
                        <p className="text-sm font-medium">Enhancing with AI...</p>
                      </div>
                    </div>
                  )}
                  
                  {/* Enhanced Badge */}
                  {isEnhanced && !isProcessing && (
                    <div className="absolute top-4 right-4 bg-green-500 text-white px-3 py-1.5 rounded-full flex items-center gap-2 shadow-lg">
                      <Check className="size-4" />
                      <span className="text-sm font-medium">Enhanced</span>
                    </div>
                  )}
                </div>
              </div>

              {/* Enhancement Options */}
              {!isEnhanced && !isProcessing && (
                <div className="bg-white rounded-xl p-4 shadow-sm">
                  <h3 className="font-semibold mb-3 text-sm flex items-center gap-2">
                    <Sparkles className="size-4 text-purple-600" />
                    Enhancement Options
                  </h3>
                  <div className="grid grid-cols-2 gap-3">
                    {enhancements.map((enhancement) => {
                      const Icon = enhancement.icon;
                      return (
                        <button
                          key={enhancement.id}
                          onClick={() => applyEnhancement(enhancement.id)}
                          className="p-4 rounded-xl border-2 border-neutral-200 hover:border-purple-300 hover:bg-purple-50 transition-all text-left"
                        >
                          <div className="flex items-start gap-3">
                            <div className="p-2 bg-purple-100 rounded-lg shrink-0">
                              <Icon className="size-5 text-purple-600" />
                            </div>
                            <div className="flex-1 min-w-0">
                              <h4 className="font-medium text-sm mb-1">
                                {enhancement.name}
                              </h4>
                              <p className="text-xs text-neutral-600">
                                {enhancement.description}
                              </p>
                            </div>
                          </div>
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Info Card */}
              <div className="bg-gradient-to-br from-purple-50 to-blue-50 border border-purple-200 rounded-xl p-4">
                <h3 className="font-semibold mb-2 text-sm flex items-center gap-2">
                  <Sparkles className="size-4 text-purple-600" />
                  AI-Powered Enhancement
                </h3>
                <ul className="text-sm text-neutral-700 space-y-1">
                  <li>• Automatically improves image quality</li>
                  <li>• Enhances clarity and sharpness</li>
                  <li>• Reduces noise and artifacts</li>
                  <li>• Powered by Google Gemini AI</li>
                </ul>
              </div>

              {/* Before/After Comparison */}
              {isEnhanced && (
                <div className="bg-white rounded-xl p-4 shadow-sm">
                  <div className="flex items-center justify-between mb-3">
                    <h3 className="font-semibold text-sm">Results</h3>
                    <span className="text-xs text-green-600 font-medium flex items-center gap-1">
                      <Check className="size-3" />
                      Enhancement Applied
                    </span>
                  </div>
                  <div className="space-y-2">
                    <Button
                      onClick={downloadImage}
                      className="w-full gap-2"
                      size="lg"
                    >
                      <Download className="size-5" />
                      Download Enhanced Image
                    </Button>
                    <Button
                      variant="outline"
                      onClick={resetImage}
                      className="w-full"
                    >
                      Try Different Enhancement
                    </Button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
