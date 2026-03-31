import { useState } from "react";
import { useNavigate } from "react-router";
import {
  User,
  Mail,
  Bell,
  Moon,
  Globe,
  HelpCircle,
  Shield,
  FileText,
  LogOut,
  ChevronRight,
  Crown,
  Settings as SettingsIcon,
} from "lucide-react";
import { Button } from "../components/ui/button";
import { Switch } from "../components/ui/switch";
import { cn } from "../components/ui/utils";
import { useTheme } from "../contexts/ThemeContext";

type SettingItem = {
  id: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  type: "toggle" | "link";
  value?: boolean;
  route?: string;
};

export function Profile() {
  const navigate = useNavigate();
  const { theme, toggleTheme } = useTheme();
  const [settings, setSettings] = useState<Record<string, boolean>>({
    notifications: true,
  });

  const accountSettings: SettingItem[] = [
    {
      id: "profile",
      label: "Edit Profile",
      icon: User,
      type: "link",
      route: "/settings/profile",
    },
    {
      id: "email",
      label: "Email Settings",
      icon: Mail,
      type: "link",
      route: "/settings/email",
    },
  ];

  const appSettings: SettingItem[] = [
    {
      id: "notifications",
      label: "Notifications",
      icon: Bell,
      type: "toggle",
      value: settings.notifications,
    },
    {
      id: "darkMode",
      label: "Dark Mode",
      icon: Moon,
      type: "toggle",
      value: theme === "dark",
    },
    {
      id: "language",
      label: "Language",
      icon: Globe,
      type: "link",
      route: "/settings/language",
    },
  ];

  const otherSettings: SettingItem[] = [
    {
      id: "help",
      label: "Help & Support",
      icon: HelpCircle,
      type: "link",
      route: "/help",
    },
    {
      id: "privacy",
      label: "Privacy Policy",
      icon: Shield,
      type: "link",
      route: "/privacy",
    },
    {
      id: "terms",
      label: "Terms of Service",
      icon: FileText,
      type: "link",
      route: "/terms",
    },
  ];

  const handleToggle = (id: string) => {
    if (id === "darkMode") {
      toggleTheme();
    } else {
      setSettings((prev) => ({
        ...prev,
        [id]: !prev[id],
      }));
    }
  };

  const renderSettingItem = (item: SettingItem) => {
    const Icon = item.icon;

    if (item.type === "toggle") {
      return (
        <div className="flex items-center justify-between p-4 bg-white border border-neutral-200 rounded-xl">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-neutral-100 rounded-lg">
              <Icon className="size-5 text-neutral-700" />
            </div>
            <span className="font-medium">{item.label}</span>
          </div>
          <Switch
            checked={item.id === "darkMode" ? theme === "dark" : settings[item.id]}
            onCheckedChange={() => handleToggle(item.id)}
          />
        </div>
      );
    }

    return (
      <button
        onClick={() => item.route && navigate(item.route)}
        className="w-full flex items-center justify-between p-4 bg-white border border-neutral-200 rounded-xl hover:border-blue-300 hover:shadow-sm transition-all"
      >
        <div className="flex items-center gap-3">
          <div className="p-2 bg-neutral-100 rounded-lg">
            <Icon className="size-5 text-neutral-700" />
          </div>
          <span className="font-medium">{item.label}</span>
        </div>
        <ChevronRight className="size-5 text-neutral-400" />
      </button>
    );
  };

  return (
    <div className="h-full overflow-auto bg-neutral-50">
      <div className="p-4 space-y-6 pb-20">
        {/* Header */}
        <div>
          <h1 className="text-2xl mb-1">Profile</h1>
          <p className="text-sm text-neutral-600">
            Manage your account and settings
          </p>
        </div>

        {/* User Card */}
        <div className="bg-gradient-to-br from-blue-600 to-purple-600 rounded-2xl p-6 text-white">
          <div className="flex items-center gap-4 mb-4">
            <div className="size-16 bg-white/20 rounded-full flex items-center justify-center">
              <User className="size-8" />
            </div>
            <div>
              <h2 className="font-semibold text-lg">John Doe</h2>
              <p className="text-sm text-white/80">john.doe@example.com</p>
            </div>
          </div>
          <Button
            variant="secondary"
            className="w-full gap-2 bg-white/20 hover:bg-white/30 text-white border-white/20"
          >
            <Crown className="size-4" />
            Upgrade to Premium
          </Button>
        </div>

        {/* Account Settings */}
        <div className="space-y-3">
          <h2 className="font-semibold text-sm text-neutral-600 px-1">Account</h2>
          <div className="space-y-2">
            {accountSettings.map((item) => (
              <div key={item.id}>{renderSettingItem(item)}</div>
            ))}
          </div>
        </div>

        {/* App Settings */}
        <div className="space-y-3">
          <h2 className="font-semibold text-sm text-neutral-600 px-1">
            App Settings
          </h2>
          <div className="space-y-2">
            {appSettings.map((item) => (
              <div key={item.id}>{renderSettingItem(item)}</div>
            ))}
          </div>
        </div>

        {/* Other */}
        <div className="space-y-3">
          <h2 className="font-semibold text-sm text-neutral-600 px-1">Other</h2>
          <div className="space-y-2">
            {otherSettings.map((item) => (
              <div key={item.id}>{renderSettingItem(item)}</div>
            ))}
          </div>
        </div>

        {/* Storage Usage */}
        <div className="bg-white border border-neutral-200 rounded-xl p-4">
          <div className="flex items-center justify-between mb-3">
            <span className="font-medium">Storage</span>
            <span className="text-sm text-neutral-600">2.4 GB / 10 GB</span>
          </div>
          <div className="w-full h-2 bg-neutral-100 rounded-full overflow-hidden">
            <div className="h-full w-[24%] bg-blue-600 rounded-full"></div>
          </div>
        </div>

        {/* Logout */}
        <Button
          variant="outline"
          className="w-full gap-2 text-red-600 border-red-200 hover:bg-red-50"
        >
          <LogOut className="size-4" />
          Log Out
        </Button>

        {/* Version */}
        <p className="text-center text-xs text-neutral-500">Version 1.0.0</p>
      </div>
    </div>
  );
}