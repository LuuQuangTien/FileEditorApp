import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router";
import {
  ArrowLeft,
  Download,
  RotateCcw,
  Check,
  Crop as CropIcon,
  Maximize2,
  Square,
  Smartphone,
  Monitor,
} from "lucide-react";
import { Button } from "../components/ui/button";
import { Slider } from "../components/ui/slider";
import { cn } from "../components/ui/utils";
import { toast } from "sonner";

type AspectRatio = {
  name: string;
  ratio: number | null; // null means free aspect ratio
  icon: React.ComponentType<{ className?: string }>;
};

export function CropImage() {
  const navigate = useNavigate();
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const imageRef = useRef<HTMLImageElement>(null);
  const [image, setImage] = useState<string | null>(null);
  const [cropArea, setCropArea] = useState({ x: 50, y: 50, width: 300, height: 300 });
  const [isDragging, setIsDragging] = useState(false);
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
  const [selectedRatio, setSelectedRatio] = useState<number | null>(null);

  const aspectRatios: AspectRatio[] = [
    { name: "Free", ratio: null, icon: Maximize2 },
    { name: "1:1", ratio: 1, icon: Square },
    { name: "4:3", ratio: 4 / 3, icon: Monitor },
    { name: "16:9", ratio: 16 / 9, icon: Monitor },
    { name: "9:16", ratio: 9 / 16, icon: Smartphone },
  ];

  useEffect(() => {
    // Load a sample image
    const img = new Image();
    img.crossOrigin = "anonymous";
    img.src = "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=600&fit=crop";
    img.onload = () => {
      setImage(img.src);
      if (canvasRef.current) {
        const canvas = canvasRef.current;
        const ctx = canvas.getContext("2d");
        if (ctx) {
          canvas.width = img.width;
          canvas.height = img.height;
          ctx.drawImage(img, 0, 0);
          
          // Set initial crop area to center
          setCropArea({
            x: img.width / 2 - 150,
            y: img.height / 2 - 150,
            width: 300,
            height: 300,
          });
        }
      }
      if (imageRef.current) {
        imageRef.current.src = img.src;
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

  const handleCropAreaChange = (updates: Partial<typeof cropArea>) => {
    setCropArea({ ...cropArea, ...updates });
  };

  const applyAspectRatio = (ratio: number | null) => {
    setSelectedRatio(ratio);
    if (ratio) {
      const newHeight = cropArea.width / ratio;
      setCropArea({ ...cropArea, height: newHeight });
    }
  };

  const applyCrop = () => {
    if (!canvasRef.current || !imageRef.current) return;

    const canvas = canvasRef.current;
    const img = imageRef.current;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    // Create a new canvas for the cropped image
    const croppedCanvas = document.createElement("canvas");
    croppedCanvas.width = cropArea.width;
    croppedCanvas.height = cropArea.height;
    const croppedCtx = croppedCanvas.getContext("2d");
    
    if (croppedCtx) {
      croppedCtx.drawImage(
        img,
        cropArea.x,
        cropArea.y,
        cropArea.width,
        cropArea.height,
        0,
        0,
        cropArea.width,
        cropArea.height
      );

      // Update the main canvas
      canvas.width = cropArea.width;
      canvas.height = cropArea.height;
      ctx.drawImage(croppedCanvas, 0, 0);

      // Update the crop area to fill the new canvas
      setCropArea({ x: 0, y: 0, width: cropArea.width, height: cropArea.height });
      
      toast.success("Image cropped!");
    }
  };

  const resetCrop = () => {
    if (image && imageRef.current && canvasRef.current) {
      const img = imageRef.current;
      const canvas = canvasRef.current;
      const ctx = canvas.getContext("2d");
      if (ctx) {
        canvas.width = img.naturalWidth;
        canvas.height = img.naturalHeight;
        ctx.drawImage(img, 0, 0);
        
        setCropArea({
          x: img.naturalWidth / 2 - 150,
          y: img.naturalHeight / 2 - 150,
          width: 300,
          height: 300,
        });
      }
    }
    setSelectedRatio(null);
  };

  const downloadImage = () => {
    if (!canvasRef.current) return;
    const url = canvasRef.current.toDataURL("image/png");
    const link = document.createElement("a");
    link.download = "cropped-image.png";
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
              <h1 className="text-lg font-semibold">Crop Image</h1>
              <p className="text-xs text-neutral-600">Crop and resize your image</p>
            </div>
          </div>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={resetCrop}
              className="gap-2"
            >
              <RotateCcw className="size-4" />
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={downloadImage}
              className="gap-2"
            >
              <Download className="size-4" />
            </Button>
          </div>
        </div>
      </div>

      {/* Canvas Area */}
      <div className="flex-1 overflow-auto p-4">
        <div className="max-w-2xl mx-auto">
          {!image ? (
            <div className="bg-white rounded-xl border-2 border-dashed border-neutral-300 p-8 text-center">
              <div className="mb-4">
                <CropIcon className="size-12 mx-auto text-neutral-400" />
              </div>
              <h3 className="font-semibold mb-2">Upload an image to crop</h3>
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
              {/* Canvas with crop overlay */}
              <div className="bg-white rounded-xl p-4 shadow-sm">
                <div className="relative inline-block">
                  <canvas
                    ref={canvasRef}
                    className="max-w-full h-auto border border-neutral-200 rounded-lg"
                  />
                  <img
                    ref={imageRef}
                    src={image}
                    alt="Original"
                    className="hidden"
                  />
                  
                  {/* Crop overlay */}
                  <div
                    className="absolute border-2 border-blue-500 bg-blue-500/10"
                    style={{
                      left: `${(cropArea.x / canvasRef.current!.width) * 100}%`,
                      top: `${(cropArea.y / canvasRef.current!.height) * 100}%`,
                      width: `${(cropArea.width / canvasRef.current!.width) * 100}%`,
                      height: `${(cropArea.height / canvasRef.current!.height) * 100}%`,
                    }}
                  >
                    {/* Resize handles */}
                    <div className="absolute -right-1 -bottom-1 size-3 bg-blue-500 rounded-full cursor-nwse-resize"></div>
                  </div>
                </div>
              </div>

              {/* Aspect Ratio Selection */}
              <div className="bg-white rounded-xl p-4 shadow-sm">
                <h3 className="font-semibold mb-3 text-sm">Aspect Ratio</h3>
                <div className="grid grid-cols-5 gap-2">
                  {aspectRatios.map((aspectRatio) => {
                    const Icon = aspectRatio.icon;
                    return (
                      <button
                        key={aspectRatio.name}
                        onClick={() => applyAspectRatio(aspectRatio.ratio)}
                        className={cn(
                          "p-3 rounded-lg border-2 transition-all flex flex-col items-center gap-2",
                          selectedRatio === aspectRatio.ratio
                            ? "border-blue-500 bg-blue-50"
                            : "border-neutral-200 hover:border-neutral-300"
                        )}
                      >
                        <Icon className="size-5" />
                        <span className="text-xs font-medium">{aspectRatio.name}</span>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Crop Controls */}
              <div className="bg-white rounded-xl p-4 shadow-sm space-y-4">
                <div>
                  <label className="text-sm font-medium block mb-2">
                    Width: {Math.round(cropArea.width)}px
                  </label>
                  <Slider
                    value={[cropArea.width]}
                    onValueChange={([width]) =>
                      handleCropAreaChange({
                        width,
                        ...(selectedRatio && { height: width / selectedRatio }),
                      })
                    }
                    min={50}
                    max={canvasRef.current?.width || 800}
                    step={1}
                    className="w-full"
                  />
                </div>

                <div>
                  <label className="text-sm font-medium block mb-2">
                    Height: {Math.round(cropArea.height)}px
                  </label>
                  <Slider
                    value={[cropArea.height]}
                    onValueChange={([height]) =>
                      handleCropAreaChange({
                        height,
                        ...(selectedRatio && { width: height * selectedRatio }),
                      })
                    }
                    min={50}
                    max={canvasRef.current?.height || 600}
                    step={1}
                    className="w-full"
                    disabled={selectedRatio !== null}
                  />
                </div>
              </div>

              {/* Apply Button */}
              <Button
                onClick={applyCrop}
                className="w-full gap-2"
                size="lg"
              >
                <Check className="size-5" />
                Apply Crop
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
