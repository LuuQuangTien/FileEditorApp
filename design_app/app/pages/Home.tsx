import { useState } from "react";
import { useNavigate } from "react-router";
import {
  Camera,
  ScanText,
  Languages,
  FileText,
  Image as ImageIcon,
  FilePlus,
  Menu,
  Settings,
  Sun,
  Moon,
  ArrowRight,
  Sparkles,
  Zap,
  Shield,
} from "lucide-react";
import { Button } from "../components/ui/button";
import { cn } from "../components/ui/utils";
import { useTheme } from "../contexts/ThemeContext";

type QuickAction = {
  id: string;
  title: string;
  icon: React.ComponentType<{ className?: string }>;
  color: string;
  route: string;
};

export function Home() {
  const navigate = useNavigate();
  const { theme, toggleTheme } = useTheme();

  const quickActions: QuickAction[] = [
    {
      id: "scan",
      title: "Scan Document",
      icon: Camera,
      color: "bg-blue-500",
      route: "/scan",
    },
    {
      id: "ocr",
      title: "OCR Extract",
      icon: ScanText,
      color: "bg-purple-500",
      route: "/ocr",
    },
    {
      id: "translate",
      title: "Translate",
      icon: Languages,
      color: "bg-green-500",
      route: "/translate",
    },
  ];

  const recentFiles = [
    { id: "1", name: "Q4 Report.pdf", type: "PDF", color: "bg-red-100 text-red-600", route: "/document/1" },
    { id: "2", name: "Budget.xlsx", type: "Excel", color: "bg-green-100 text-green-600", route: "/excel/2" },
    { id: "3", name: "Proposal.docx", type: "Word", color: "bg-blue-100 text-blue-600", route: "/document/3" },
  ];

  const features = [
    {
      icon: Sparkles,
      title: "AI-Powered OCR",
      description: "Extract text with 99% accuracy",
    },
    {
      icon: Zap,
      title: "Fast Processing",
      description: "Convert files in seconds",
    },
    {
      icon: Shield,
      title: "Secure Storage",
      description: "Your files are encrypted",
    },
  ];

  return (
    <div className="h-full overflow-auto bg-gradient-to-br from-blue-50 via-white to-purple-50">
      <div className="p-4 space-y-6 pb-20">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl mb-1">Welcome back!</h1>
            <p className="text-sm text-neutral-600">
              Manage your documents with AI
            </p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={toggleTheme}
              className="p-2.5 bg-white border border-neutral-200 rounded-xl hover:bg-neutral-50 transition-colors"
            >
              {theme === "dark" ? (
                <Sun className="size-5 text-amber-500" />
              ) : (
                <Moon className="size-5 text-neutral-600" />
              )}
            </button>
            <button
              onClick={() => navigate("/settings")}
              className="p-2.5 bg-white border border-neutral-200 rounded-xl hover:bg-neutral-50 transition-colors"
            >
              <Settings className="size-5 text-neutral-600" />
            </button>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="space-y-3">
          <h2 className="font-semibold">Quick Actions</h2>
          <div className="grid grid-cols-3 gap-3">
            {quickActions.map((action) => {
              const Icon = action.icon;
              return (
                <button
                  key={action.id}
                  onClick={() => navigate(action.route)}
                  className="bg-white border border-neutral-200 rounded-2xl p-4 flex flex-col items-center gap-3 hover:border-blue-300 hover:shadow-sm transition-all active:scale-95"
                >
                  <div className={cn("p-3 rounded-xl text-white", action.color)}>
                    <Icon className="size-6" />
                  </div>
                  <span className="text-sm text-center">{action.title}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Recent Files */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="font-semibold">Recent Files</h2>
            <button
              onClick={() => navigate("/files")}
              className="text-sm text-blue-600 flex items-center gap-1 hover:gap-2 transition-all"
            >
              View All
              <ArrowRight className="size-4" />
            </button>
          </div>
          <div className="space-y-2">
            {recentFiles.map((file) => (
              <button
                key={file.id}
                onClick={() => navigate(file.route)}
                className="w-full bg-white border border-neutral-200 rounded-xl p-3 flex items-center gap-3 hover:border-blue-300 hover:shadow-sm transition-all"
              >
                <div className={cn("p-2 rounded-lg text-sm", file.color)}>
                  <FileText className="size-5" />
                </div>
                <div className="flex-1 text-left">
                  <p className="font-medium text-sm">{file.name}</p>
                  <p className="text-xs text-neutral-500">{file.type} • 2 days ago</p>
                </div>
                <ArrowRight className="size-4 text-neutral-400" />
              </button>
            ))}
          </div>
        </div>

        {/* Features */}
        <div className="bg-gradient-to-br from-blue-600 to-purple-600 rounded-2xl p-5 text-white space-y-4">
          <h2 className="font-semibold text-lg">Powered by AI</h2>
          <div className="space-y-3">
            {features.map((feature, index) => {
              const Icon = feature.icon;
              return (
                <div key={index} className="flex items-start gap-3">
                  <div className="p-2 bg-white/20 rounded-lg shrink-0">
                    <Icon className="size-4" />
                  </div>
                  <div>
                    <p className="font-medium text-sm">{feature.title}</p>
                    <p className="text-xs text-white/80">{feature.description}</p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Storage Info */}
        <div className="bg-white border border-neutral-200 rounded-2xl p-4">
          <div className="flex items-center justify-between mb-2">
            <p className="text-sm font-medium">Storage Used</p>
            <p className="text-sm text-neutral-600">2.4 GB / 10 GB</p>
          </div>
          <div className="w-full h-2 bg-neutral-100 rounded-full overflow-hidden">
            <div className="h-full w-[24%] bg-gradient-to-r from-blue-500 to-purple-500 rounded-full"></div>
          </div>
        </div>
      </div>
    </div>
  );
}