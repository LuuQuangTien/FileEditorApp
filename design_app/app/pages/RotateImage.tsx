import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router";
import {
  ArrowLeft,
  Download,
  RotateCcw,
  RotateCw,
  FlipHorizontal,
  FlipVertical,
  Check,
} from "lucide-react";
import { Button } from "../components/ui/button";
import { Slider } from "../components/ui/slider";
import { cn } from "../components/ui/utils";
import { toast } from "sonner";

export function RotateImage() {
  const navigate = useNavigate();
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [image, setImage] = useState<HTMLImageElement | null>(null);
  const [rotation, setRotation] = useState(0);
  const [flipH, setFlipH] = useState(false);
  const [flipV, setFlipV] = useState(false);

  useEffect(() => {
    // Load a sample image
    const img = new Image();
    img.crossOrigin = "anonymous";
    img.src = "https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=800&h=600&fit=crop";
    img.onload = () => {
      setImage(img);
      drawImage(img, 0, false, false);
    };
  }, []);

  const drawImage = (
    img: HTMLImageElement,
    angle: number,
    flipHorizontal: boolean,
    flipVertical: boolean
  ) => {
    if (!canvasRef.current) return;

    const canvas = canvasRef.current;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    // Calculate new canvas size based on rotation
    const angleRad = (angle * Math.PI) / 180;
    const sin = Math.abs(Math.sin(angleRad));
    const cos = Math.abs(Math.cos(angleRad));
    const newWidth = img.width * cos + img.height * sin;
    const newHeight = img.width * sin + img.height * cos;

    canvas.width = newWidth;
    canvas.height = newHeight;

    // Clear canvas
    ctx.clearRect(0, 0, newWidth, newHeight);

    // Save context state
    ctx.save();

    // Move to center
    ctx.translate(newWidth / 2, newHeight / 2);

    // Apply rotation
    ctx.rotate(angleRad);

    // Apply flips
    ctx.scale(flipHorizontal ? -1 : 1, flipVertical ? -1 : 1);

    // Draw image centered
    ctx.drawImage(img, -img.width / 2, -img.height / 2);

    // Restore context state
    ctx.restore();
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        const img = new Image();
        img.onload = () => {
          setImage(img);
          setRotation(0);
          setFlipH(false);
          setFlipV(false);
          drawImage(img, 0, false, false);
        };
        img.src = event.target?.result as string;
      };
      reader.readAsDataURL(file);
    }
  };

  const handleRotationChange = (value: number) => {
    setRotation(value);
    if (image) {
      drawImage(image, value, flipH, flipV);
    }
  };

  const rotate90CW = () => {
    const newRotation = (rotation + 90) % 360;
    handleRotationChange(newRotation);
  };

  const rotate90CCW = () => {
    const newRotation = (rotation - 90 + 360) % 360;
    handleRotationChange(newRotation);
  };

  const toggleFlipH = () => {
    const newFlipH = !flipH;
    setFlipH(newFlipH);
    if (image) {
      drawImage(image, rotation, newFlipH, flipV);
    }
  };

  const toggleFlipV = () => {
    const newFlipV = !flipV;
    setFlipV(newFlipV);
    if (image) {
      drawImage(image, rotation, flipH, newFlipV);
    }
  };

  const resetTransform = () => {
    setRotation(0);
    setFlipH(false);
    setFlipV(false);
    if (image) {
      drawImage(image, 0, false, false);
    }
    toast.success("Reset to original!");
  };

  const downloadImage = () => {
    if (!canvasRef.current) return;
    const url = canvasRef.current.toDataURL("image/png");
    const link = document.createElement("a");
    link.download = "rotated-image.png";
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
              <h1 className="text-lg font-semibold">Rotate Image</h1>
              <p className="text-xs text-neutral-600">Rotate and flip your image</p>
            </div>
          </div>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={resetTransform}
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
                <RotateCw className="size-12 mx-auto text-neutral-400" />
              </div>
              <h3 className="font-semibold mb-2">Upload an image to rotate</h3>
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
              {/* Canvas */}
              <div className="bg-white rounded-xl p-4 shadow-sm">
                <div className="flex justify-center items-center min-h-[300px] bg-neutral-50 rounded-lg overflow-auto">
                  <canvas
                    ref={canvasRef}
                    className="max-w-full h-auto"
                  />
                </div>
              </div>

              {/* Quick Rotation Buttons */}
              <div className="bg-white rounded-xl p-4 shadow-sm">
                <h3 className="font-semibold mb-3 text-sm">Quick Actions</h3>
                <div className="grid grid-cols-4 gap-2">
                  <button
                    onClick={rotate90CCW}
                    className="p-4 rounded-lg border-2 border-neutral-200 hover:border-blue-300 hover:bg-blue-50 transition-all flex flex-col items-center gap-2"
                  >
                    <RotateCcw className="size-6 text-blue-600" />
                    <span className="text-xs font-medium">90° CCW</span>
                  </button>
                  <button
                    onClick={rotate90CW}
                    className="p-4 rounded-lg border-2 border-neutral-200 hover:border-blue-300 hover:bg-blue-50 transition-all flex flex-col items-center gap-2"
                  >
                    <RotateCw className="size-6 text-blue-600" />
                    <span className="text-xs font-medium">90° CW</span>
                  </button>
                  <button
                    onClick={toggleFlipH}
                    className={cn(
                      "p-4 rounded-lg border-2 transition-all flex flex-col items-center gap-2",
                      flipH
                        ? "border-blue-500 bg-blue-50"
                        : "border-neutral-200 hover:border-blue-300 hover:bg-blue-50"
                    )}
                  >
                    <FlipHorizontal className="size-6 text-blue-600" />
                    <span className="text-xs font-medium">Flip H</span>
                  </button>
                  <button
                    onClick={toggleFlipV}
                    className={cn(
                      "p-4 rounded-lg border-2 transition-all flex flex-col items-center gap-2",
                      flipV
                        ? "border-blue-500 bg-blue-50"
                        : "border-neutral-200 hover:border-blue-300 hover:bg-blue-50"
                    )}
                  >
                    <FlipVertical className="size-6 text-blue-600" />
                    <span className="text-xs font-medium">Flip V</span>
                  </button>
                </div>
              </div>

              {/* Fine Rotation Control */}
              <div className="bg-white rounded-xl p-4 shadow-sm space-y-4">
                <div>
                  <label className="text-sm font-medium block mb-2">
                    Rotation: {rotation}°
                  </label>
                  <Slider
                    value={[rotation]}
                    onValueChange={([value]) => handleRotationChange(value)}
                    min={0}
                    max={360}
                    step={1}
                    className="w-full"
                  />
                  <div className="flex justify-between text-xs text-neutral-600 mt-1">
                    <span>0°</span>
                    <span>180°</span>
                    <span>360°</span>
                  </div>
                </div>
              </div>

              {/* Info Card */}
              <div className="bg-blue-50 rounded-xl p-4">
                <h3 className="font-semibold mb-2 text-sm flex items-center gap-2">
                  💡 Tips
                </h3>
                <ul className="text-sm text-neutral-700 space-y-1">
                  <li>• Use 90° buttons for quick rotations</li>
                  <li>• Drag the slider for precise angle control</li>
                  <li>• Flip horizontally or vertically to mirror image</li>
                  <li>• Click reset to restore original orientation</li>
                </ul>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
