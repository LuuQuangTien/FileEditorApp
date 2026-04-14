import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router";
import {
  ArrowLeft,
  Download,
  RotateCcw,
  Wand2,
  Sun,
  Contrast,
  Droplets,
  Palette,
} from "lucide-react";
import { Button } from "../components/ui/button";
import { Slider } from "../components/ui/slider";
import { cn } from "../components/ui/utils";
import { toast } from "sonner";

type Filter = {
  id: string;
  name: string;
  filter: string;
};

type Adjustment = {
  brightness: number;
  contrast: number;
  saturation: number;
  blur: number;
};

export function ImageFilter() {
  const navigate = useNavigate();
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const originalImageRef = useRef<HTMLImageElement | null>(null);
  const [image, setImage] = useState<string | null>(null);
  const [selectedFilter, setSelectedFilter] = useState<string | null>(null);
  const [adjustments, setAdjustments] = useState<Adjustment>({
    brightness: 100,
    contrast: 100,
    saturation: 100,
    blur: 0,
  });

  const filters: Filter[] = [
    { id: "none", name: "Original", filter: "none" },
    { id: "grayscale", name: "B&W", filter: "grayscale(100%)" },
    { id: "sepia", name: "Sepia", filter: "sepia(100%)" },
    { id: "vintage", name: "Vintage", filter: "sepia(50%) contrast(110%) brightness(110%)" },
    { id: "cool", name: "Cool", filter: "saturate(120%) hue-rotate(-15deg)" },
    { id: "warm", name: "Warm", filter: "saturate(120%) hue-rotate(15deg)" },
    { id: "vivid", name: "Vivid", filter: "saturate(150%) contrast(120%)" },
    { id: "fade", name: "Fade", filter: "brightness(110%) contrast(90%) saturate(80%)" },
  ];

  useEffect(() => {
    // Load a sample image
    const img = new Image();
    img.crossOrigin = "anonymous";
    img.src = "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=600&fit=crop";
    img.onload = () => {
      originalImageRef.current = img;
      setImage(img.src);
      drawImage(img);
    };
  }, []);

  const drawImage = (img: HTMLImageElement) => {
    if (!canvasRef.current) return;

    const canvas = canvasRef.current;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    canvas.width = img.width;
    canvas.height = img.height;
    ctx.drawImage(img, 0, 0);
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        const img = new Image();
        img.onload = () => {
          originalImageRef.current = img;
          setImage(event.target?.result as string);
          drawImage(img);
          setSelectedFilter(null);
          setAdjustments({
            brightness: 100,
            contrast: 100,
            saturation: 100,
            blur: 0,
          });
        };
        img.src = event.target?.result as string;
      };
      reader.readAsDataURL(file);
    }
  };

  const applyFilter = (filterId: string) => {
    setSelectedFilter(filterId);
  };

  const handleAdjustmentChange = (key: keyof Adjustment, value: number) => {
    setAdjustments({
      ...adjustments,
      [key]: value,
    });
  };

  const getFilterString = () => {
    const filter = filters.find((f) => f.id === selectedFilter);
    const baseFilter = filter?.filter || "none";
    
    const customFilters = [
      `brightness(${adjustments.brightness}%)`,
      `contrast(${adjustments.contrast}%)`,
      `saturate(${adjustments.saturation}%)`,
      adjustments.blur > 0 ? `blur(${adjustments.blur}px)` : "",
    ].filter(Boolean).join(" ");

    return baseFilter === "none" ? customFilters : `${baseFilter} ${customFilters}`;
  };

  const resetAll = () => {
    setSelectedFilter(null);
    setAdjustments({
      brightness: 100,
      contrast: 100,
      saturation: 100,
      blur: 0,
    });
    toast.success("Reset to original!");
  };

  const downloadImage = () => {
    if (!canvasRef.current || !originalImageRef.current) return;

    // Create a temporary canvas to apply filters
    const tempCanvas = document.createElement("canvas");
    const tempCtx = tempCanvas.getContext("2d");
    if (!tempCtx) return;

    const img = originalImageRef.current;
    tempCanvas.width = img.width;
    tempCanvas.height = img.height;

    // Apply filter
    tempCtx.filter = getFilterString();
    tempCtx.drawImage(img, 0, 0);

    const url = tempCanvas.toDataURL("image/png");
    const link = document.createElement("a");
    link.download = "filtered-image.png";
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
              <h1 className="text-lg font-semibold">Apply Filters</h1>
              <p className="text-xs text-neutral-600">Enhance with filters and adjustments</p>
            </div>
          </div>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={resetAll}
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
      <div className="flex-1 overflow-auto p-4 pb-20">
        <div className="max-w-2xl mx-auto">
          {!image ? (
            <div className="bg-white rounded-xl border-2 border-dashed border-neutral-300 p-8 text-center">
              <div className="mb-4">
                <Wand2 className="size-12 mx-auto text-neutral-400" />
              </div>
              <h3 className="font-semibold mb-2">Upload an image to filter</h3>
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
              {/* Canvas with Preview */}
              <div className="bg-white rounded-xl p-4 shadow-sm">
                <div className="flex justify-center items-center bg-neutral-50 rounded-lg overflow-hidden">
                  <img
                    src={image}
                    alt="Filtered"
                    className="max-w-full h-auto"
                    style={{ filter: getFilterString() }}
                  />
                  <canvas ref={canvasRef} className="hidden" />
                </div>
              </div>

              {/* Filter Presets */}
              <div className="bg-white rounded-xl p-4 shadow-sm">
                <h3 className="font-semibold mb-3 text-sm flex items-center gap-2">
                  <Palette className="size-4" />
                  Filter Presets
                </h3>
                <div className="grid grid-cols-4 gap-2">
                  {filters.map((filter) => (
                    <button
                      key={filter.id}
                      onClick={() => applyFilter(filter.id)}
                      className={cn(
                        "relative rounded-lg overflow-hidden border-2 transition-all",
                        selectedFilter === filter.id
                          ? "border-blue-500 ring-2 ring-blue-200"
                          : "border-neutral-200 hover:border-neutral-300"
                      )}
                    >
                      <img
                        src={image}
                        alt={filter.name}
                        className="w-full h-16 object-cover"
                        style={{ filter: filter.filter }}
                      />
                      <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/70 to-transparent p-1.5">
                        <span className="text-xs font-medium text-white block text-center">
                          {filter.name}
                        </span>
                      </div>
                    </button>
                  ))}
                </div>
              </div>

              {/* Adjustments */}
              <div className="bg-white rounded-xl p-4 shadow-sm space-y-4">
                <h3 className="font-semibold text-sm">Fine Adjustments</h3>

                <div>
                  <label className="text-sm font-medium flex items-center gap-2 mb-2">
                    <Sun className="size-4 text-yellow-600" />
                    Brightness: {adjustments.brightness}%
                  </label>
                  <Slider
                    value={[adjustments.brightness]}
                    onValueChange={([value]) =>
                      handleAdjustmentChange("brightness", value)
                    }
                    min={0}
                    max={200}
                    step={1}
                    className="w-full"
                  />
                </div>

                <div>
                  <label className="text-sm font-medium flex items-center gap-2 mb-2">
                    <Contrast className="size-4 text-blue-600" />
                    Contrast: {adjustments.contrast}%
                  </label>
                  <Slider
                    value={[adjustments.contrast]}
                    onValueChange={([value]) =>
                      handleAdjustmentChange("contrast", value)
                    }
                    min={0}
                    max={200}
                    step={1}
                    className="w-full"
                  />
                </div>

                <div>
                  <label className="text-sm font-medium flex items-center gap-2 mb-2">
                    <Palette className="size-4 text-purple-600" />
                    Saturation: {adjustments.saturation}%
                  </label>
                  <Slider
                    value={[adjustments.saturation]}
                    onValueChange={([value]) =>
                      handleAdjustmentChange("saturation", value)
                    }
                    min={0}
                    max={200}
                    step={1}
                    className="w-full"
                  />
                </div>

                <div>
                  <label className="text-sm font-medium flex items-center gap-2 mb-2">
                    <Droplets className="size-4 text-cyan-600" />
                    Blur: {adjustments.blur}px
                  </label>
                  <Slider
                    value={[adjustments.blur]}
                    onValueChange={([value]) =>
                      handleAdjustmentChange("blur", value)
                    }
                    min={0}
                    max={10}
                    step={0.5}
                    className="w-full"
                  />
                </div>
              </div>

              {/* Info Card */}
              <div className="bg-purple-50 rounded-xl p-4">
                <h3 className="font-semibold mb-2 text-sm flex items-center gap-2">
                  💡 Tips
                </h3>
                <ul className="text-sm text-neutral-700 space-y-1">
                  <li>• Choose a preset filter for quick styling</li>
                  <li>• Fine-tune with brightness and contrast</li>
                  <li>• Increase saturation for vibrant colors</li>
                  <li>• Add subtle blur for dreamy effect</li>
                </ul>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
