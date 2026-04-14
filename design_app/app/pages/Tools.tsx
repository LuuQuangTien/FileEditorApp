import { useState } from "react";
import { useNavigate } from "react-router";
import {
  FileImage,
  FilePlus,
  Combine,
  Split,
  FileType,
  Image as ImageIcon,
  Crop,
  RotateCw,
  Wand2,
  Sparkles,
  ArrowRight,
  Highlighter,
  MessageSquare,
} from "lucide-react";
import { Button } from "../components/ui/button";
import { cn } from "../components/ui/utils";

type ToolCategory = {
  id: string;
  title: string;
  icon: React.ComponentType<{ className?: string }>;
  color: string;
  tools: Tool[];
};

type Tool = {
  id: string;
  name: string;
  description: string;
  icon: React.ComponentType<{ className?: string }>;
  route: string;
};

export function Tools() {
  const navigate = useNavigate();

  const toolCategories: ToolCategory[] = [
    {
      id: "document",
      title: "Document Tools",
      icon: FilePlus,
      color: "bg-blue-500",
      tools: [
        {
          id: "pdf-word",
          name: "PDF to Word",
          description: "Convert PDF to editable DOCX",
          icon: FileType,
          route: "/tool/pdf-to-word",
        },
        {
          id: "pdf-excel",
          name: "PDF to Excel",
          description: "Extract tables from PDF",
          icon: FileType,
          route: "/tool/pdf-to-excel",
        },
        {
          id: "image-pdf",
          name: "Image to PDF",
          description: "Convert images to PDF",
          icon: FileImage,
          route: "/tool/image-to-pdf",
        },
        {
          id: "merge-pdf",
          name: "Merge PDF",
          description: "Combine multiple PDFs",
          icon: Combine,
          route: "/tool/merge-pdf",
        },
        {
          id: "split-pdf",
          name: "Split PDF",
          description: "Split PDF into pages",
          icon: Split,
          route: "/tool/split-pdf",
        },
        {
          id: "annotate",
          name: "Annotate PDF",
          description: "Highlight & add notes",
          icon: Highlighter,
          route: "/tool/annotate-pdf",
        },
      ],
    },
    {
      id: "image",
      title: "Image Tools",
      icon: ImageIcon,
      color: "bg-purple-500",
      tools: [
        {
          id: "crop",
          name: "Crop Image",
          description: "Crop and resize images",
          icon: Crop,
          route: "/tool/crop-image",
        },
        {
          id: "rotate",
          name: "Rotate Image",
          description: "Rotate and flip images",
          icon: RotateCw,
          route: "/tool/rotate-image",
        },
        {
          id: "filter",
          name: "Apply Filters",
          description: "Add filters and effects",
          icon: Wand2,
          route: "/tool/image-filter",
        },
        {
          id: "scan",
          name: "Document Scanner",
          description: "Scan docs with camera",
          icon: FileImage,
          route: "/scan",
        },
        {
          id: "enhance",
          name: "Enhance Image",
          description: "Improve clarity & quality",
          icon: Sparkles,
          route: "/tool/enhance-image",
        },
      ],
    },
  ];

  return (
    <div className="h-full overflow-auto bg-neutral-50">
      <div className="p-4 space-y-6 pb-20">
        {/* Header */}
        <div>
          <h1 className="text-2xl mb-1">Tools</h1>
          <p className="text-sm text-neutral-600">
            Convert, edit, and enhance your files
          </p>
        </div>

        {/* Tool Categories */}
        {toolCategories.map((category) => {
          const CategoryIcon = category.icon;
          return (
            <div key={category.id} className="space-y-3">
              <div className="flex items-center gap-2">
                <div className={cn("p-2 rounded-lg text-white", category.color)}>
                  <CategoryIcon className="size-4" />
                </div>
                <h2 className="font-semibold">{category.title}</h2>
              </div>

              <div className="grid grid-cols-2 gap-3">
                {category.tools.map((tool) => {
                  const ToolIcon = tool.icon;
                  return (
                    <button
                      key={tool.id}
                      onClick={() => navigate(tool.route)}
                      className="bg-white border border-neutral-200 rounded-xl p-4 text-left hover:border-blue-300 hover:shadow-sm transition-all active:scale-95"
                    >
                      <div className="flex items-start justify-between mb-3">
                        <div className="p-2 bg-neutral-100 rounded-lg">
                          <ToolIcon className="size-5 text-neutral-700" />
                        </div>
                        <ArrowRight className="size-4 text-neutral-400" />
                      </div>
                      <h3 className="font-medium text-sm mb-1">{tool.name}</h3>
                      <p className="text-xs text-neutral-600">{tool.description}</p>
                    </button>
                  );
                })}
              </div>
            </div>
          );
        })}

        {/* Premium Banner */}
        <div className="bg-gradient-to-br from-amber-500 to-orange-600 rounded-2xl p-5 text-white">
          <div className="flex items-start gap-3">
            <div className="p-2 bg-white/20 rounded-lg shrink-0">
              <Sparkles className="size-5" />
            </div>
            <div className="flex-1">
              <h3 className="font-semibold mb-1">Unlock Premium</h3>
              <p className="text-sm text-white/90 mb-3">
                Get unlimited conversions, batch processing, and more
              </p>
              <Button
                size="sm"
                className="bg-white text-orange-600 hover:bg-white/90"
              >
                Upgrade Now
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
