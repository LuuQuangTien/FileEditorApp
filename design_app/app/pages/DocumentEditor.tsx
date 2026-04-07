import { useState } from "react";
import { useNavigate, useParams } from "react-router";
import {
  ArrowLeft,
  Save,
  Share2,
  MoreVertical,
  Bold,
  Italic,
  Underline,
  AlignLeft,
  AlignCenter,
  AlignRight,
  List,
  ListOrdered,
  Image as ImageIcon,
  Sparkles,
  Wand2,
  Check,
  X,
} from "lucide-react";
import { Button } from "../components/ui/button";
import { Textarea } from "../components/ui/textarea";
import { cn } from "../components/ui/utils";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "../components/ui/dropdown-menu";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "../components/ui/sheet";
import { toast } from "sonner";

export function DocumentEditor() {
  const navigate = useNavigate();
  const { id } = useParams();
  const [content, setContent] = useState(
    "This is a sample document. You can edit this text and use AI features to improve your writing.\n\nClick the AI button to get writing assistance, grammar corrections, and content suggestions."
  );
  const [showAI, setShowAI] = useState(false);
  const [aiSuggestion, setAiSuggestion] = useState("");
  const [isGenerating, setIsGenerating] = useState(false);
  const [selectedText, setSelectedText] = useState("");
  const [formatOptions, setFormatOptions] = useState({
    bold: false,
    italic: false,
    underline: false,
  });

  const handleAIAssist = (action: string) => {
    setIsGenerating(true);
    setShowAI(true);

    // Simulate AI processing
    setTimeout(() => {
      let suggestion = "";
      
      switch (action) {
        case "improve":
          suggestion = "Enhanced version:\n\nThis document serves as an exemplary template. Feel free to modify this content and leverage advanced AI capabilities to refine your composition.\n\nAccess the AI assistant to receive professional writing guidance, comprehensive grammar corrections, and intelligent content recommendations.";
          break;
        case "fix":
          suggestion = "Grammar corrections applied:\n- Fixed punctuation\n- Corrected spelling\n- Improved sentence structure\n\nYour text is now grammatically correct!";
          break;
        case "shorten":
          suggestion = "Condensed version:\n\nEditable sample document. Use AI features for writing improvements.\n\nClick AI button for assistance, corrections, and suggestions.";
          break;
        case "expand":
          suggestion = "Expanded version:\n\nThis comprehensive document serves as a detailed sample template for your editing needs. You have complete freedom to modify and customize this text according to your requirements. Additionally, you can harness the power of artificial intelligence features to significantly enhance and improve the quality of your writing.\n\nBy clicking the AI assistance button, you'll gain access to professional writing support, including detailed grammar corrections, style improvements, and intelligent content suggestions tailored to your specific needs.";
          break;
        case "translate":
          suggestion = "Translated to Vietnamese:\n\nĐây là một tài liệu mẫu. Bạn có thể chỉnh sửa văn bản này và sử dụng các tính năng AI để cải thiện bài viết của mình.\n\nNhấp vào nút AI để nhận trợ giúp viết, sửa lỗi ngữ pháp và đề xuất nội dung.";
          break;
        default:
          suggestion = "AI suggestion will appear here based on your selection.";
      }
      
      setAiSuggestion(suggestion);
      setIsGenerating(false);
    }, 1500);
  };

  const applyAISuggestion = () => {
    setContent(aiSuggestion.split("\n").slice(1).join("\n"));
    setShowAI(false);
    toast.success("AI suggestion applied!");
  };

  const handleSave = () => {
    toast.success("Document saved!");
  };

  const handleTextSelection = () => {
    const textarea = document.querySelector("textarea");
    if (textarea) {
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      const selected = content.substring(start, end);
      if (selected) {
        setSelectedText(selected);
      }
    }
  };

  return (
    <div className="h-full flex flex-col bg-white">
      {/* Header */}
      <div className="border-b border-neutral-200 px-4 py-3">
        <div className="flex items-center justify-between mb-3">
          <button
            onClick={() => navigate("/")}
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
                <DropdownMenuItem>
                  <Share2 className="size-4 mr-2" />
                  Share
                </DropdownMenuItem>
                <DropdownMenuItem>Export as PDF</DropdownMenuItem>
                <DropdownMenuItem>Download</DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
        <h1 className="text-lg font-semibold truncate">
          Project Proposal.docx
        </h1>
      </div>

      {/* Formatting Toolbar */}
      <div className="border-b border-neutral-200 px-4 py-2 overflow-x-auto">
        <div className="flex items-center gap-1 min-w-max">
          <button
            onClick={() =>
              setFormatOptions({ ...formatOptions, bold: !formatOptions.bold })
            }
            className={cn(
              "p-2 rounded-lg transition-colors",
              formatOptions.bold
                ? "bg-blue-100 text-blue-600"
                : "hover:bg-neutral-100"
            )}
          >
            <Bold className="size-5" />
          </button>
          <button
            onClick={() =>
              setFormatOptions({
                ...formatOptions,
                italic: !formatOptions.italic,
              })
            }
            className={cn(
              "p-2 rounded-lg transition-colors",
              formatOptions.italic
                ? "bg-blue-100 text-blue-600"
                : "hover:bg-neutral-100"
            )}
          >
            <Italic className="size-5" />
          </button>
          <button
            onClick={() =>
              setFormatOptions({
                ...formatOptions,
                underline: !formatOptions.underline,
              })
            }
            className={cn(
              "p-2 rounded-lg transition-colors",
              formatOptions.underline
                ? "bg-blue-100 text-blue-600"
                : "hover:bg-neutral-100"
            )}
          >
            <Underline className="size-5" />
          </button>
          <div className="w-px h-6 bg-neutral-200 mx-1"></div>
          <button className="p-2 rounded-lg hover:bg-neutral-100">
            <AlignLeft className="size-5" />
          </button>
          <button className="p-2 rounded-lg hover:bg-neutral-100">
            <AlignCenter className="size-5" />
          </button>
          <button className="p-2 rounded-lg hover:bg-neutral-100">
            <AlignRight className="size-5" />
          </button>
          <div className="w-px h-6 bg-neutral-200 mx-1"></div>
          <button className="p-2 rounded-lg hover:bg-neutral-100">
            <List className="size-5" />
          </button>
          <button className="p-2 rounded-lg hover:bg-neutral-100">
            <ListOrdered className="size-5" />
          </button>
          <div className="w-px h-6 bg-neutral-200 mx-1"></div>
          <button className="p-2 rounded-lg hover:bg-neutral-100">
            <ImageIcon className="size-5" />
          </button>
        </div>
      </div>

      {/* Editor */}
      <div className="flex-1 overflow-auto">
        <Textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          onMouseUp={handleTextSelection}
          className="w-full h-full resize-none border-0 focus-visible:ring-0 p-4 text-base leading-relaxed"
          placeholder="Start typing..."
        />
      </div>

      {/* AI Assistant FAB */}
      <button
        onClick={() => setShowAI(true)}
        className="fixed bottom-20 right-4 size-14 bg-gradient-to-r from-purple-600 to-blue-600 text-white rounded-full shadow-lg flex items-center justify-center active:scale-95 transition-transform"
      >
        <Sparkles className="size-6" />
      </button>

      {/* AI Assistant Sheet */}
      <Sheet open={showAI} onOpenChange={setShowAI}>
        <SheetContent side="bottom" className="h-[80vh]">
          <SheetHeader className="mb-4">
            <SheetTitle className="flex items-center gap-2">
              <Wand2 className="size-5 text-purple-600" />
              AI Writing Assistant
            </SheetTitle>
            <SheetDescription>
              Select an action to improve your document
            </SheetDescription>
          </SheetHeader>

          <div className="space-y-4 overflow-auto h-[calc(100%-80px)]">
            {/* AI Actions */}
            <div className="grid grid-cols-2 gap-2">
              <Button
                variant="outline"
                onClick={() => handleAIAssist("improve")}
                className="h-auto py-3 flex flex-col gap-1"
                disabled={isGenerating}
              >
                <Sparkles className="size-5 text-purple-600" />
                <span className="text-sm">Improve Writing</span>
              </Button>
              <Button
                variant="outline"
                onClick={() => handleAIAssist("fix")}
                className="h-auto py-3 flex flex-col gap-1"
                disabled={isGenerating}
              >
                <Check className="size-5 text-green-600" />
                <span className="text-sm">Fix Grammar</span>
              </Button>
              <Button
                variant="outline"
                onClick={() => handleAIAssist("shorten")}
                className="h-auto py-3 flex flex-col gap-1"
                disabled={isGenerating}
              >
                <span className="text-lg">📝</span>
                <span className="text-sm">Make Shorter</span>
              </Button>
              <Button
                variant="outline"
                onClick={() => handleAIAssist("expand")}
                className="h-auto py-3 flex flex-col gap-1"
                disabled={isGenerating}
              >
                <span className="text-lg">📄</span>
                <span className="text-sm">Make Longer</span>
              </Button>
              <Button
                variant="outline"
                onClick={() => handleAIAssist("translate")}
                className="h-auto py-3 flex flex-col gap-1"
                disabled={isGenerating}
              >
                <span className="text-lg">🌐</span>
                <span className="text-sm">Translate</span>
              </Button>
              <Button
                variant="outline"
                className="h-auto py-3 flex flex-col gap-1"
              >
                <ImageIcon className="size-5 text-blue-600" />
                <span className="text-sm">Edit Image</span>
              </Button>
            </div>

            {/* AI Suggestion Display */}
            {(aiSuggestion || isGenerating) && (
              <div className="bg-neutral-50 rounded-xl p-4 space-y-3">
                <div className="flex items-center justify-between">
                  <h3 className="font-semibold">AI Suggestion</h3>
                  {!isGenerating && (
                    <div className="flex gap-2">
                      <Button
                        size="sm"
                        onClick={applyAISuggestion}
                        className="gap-1"
                      >
                        <Check className="size-4" />
                        Apply
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => setAiSuggestion("")}
                      >
                        <X className="size-4" />
                      </Button>
                    </div>
                  )}
                </div>
                {isGenerating ? (
                  <div className="flex items-center gap-3 py-4">
                    <div className="flex gap-1">
                      <div className="size-2 bg-purple-600 rounded-full animate-bounce"></div>
                      <div className="size-2 bg-purple-600 rounded-full animate-bounce [animation-delay:0.2s]"></div>
                      <div className="size-2 bg-purple-600 rounded-full animate-bounce [animation-delay:0.4s]"></div>
                    </div>
                    <span className="text-sm text-neutral-600">
                      Generating suggestion...
                    </span>
                  </div>
                ) : (
                  <p className="text-sm whitespace-pre-wrap">{aiSuggestion}</p>
                )}
              </div>
            )}

            {/* Tips */}
            <div className="bg-blue-50 rounded-xl p-4">
              <h3 className="font-semibold mb-2 text-sm">💡 Tips</h3>
              <ul className="text-sm text-neutral-600 space-y-1">
                <li>• Select text to get specific suggestions</li>
                <li>• Use "Improve Writing" for better clarity</li>
                <li>• "Fix Grammar" catches errors automatically</li>
                <li>• Edit images by enhancing quality or removing backgrounds</li>
              </ul>
            </div>
          </div>
        </SheetContent>
      </Sheet>
    </div>
  );
}
