import { Outlet, Link, useLocation, useNavigate } from "react-router";
import { Home, FolderOpen, Wrench, User, Camera } from "lucide-react";
import { cn } from "./ui/utils";

export function Layout() {
  const location = useLocation();
  const navigate = useNavigate();

  const navItems = [
    { path: "/", icon: Home, label: "Home" },
    { path: "/files", icon: FolderOpen, label: "Files" },
    { path: "/tools", icon: Wrench, label: "Tools" },
    { path: "/profile", icon: User, label: "Profile" },
  ];

  return (
    <div className="flex flex-col h-screen bg-neutral-50">
      {/* Main Content */}
      <main className="flex-1 overflow-auto pb-16">
        <Outlet />
      </main>

      {/* Floating Scan Button */}
      <button
        onClick={() => navigate("/scan")}
        className="fixed bottom-24 right-4 size-14 bg-gradient-to-br from-blue-600 to-purple-600 text-white rounded-full shadow-lg flex items-center justify-center active:scale-95 transition-transform z-50 hover:shadow-xl"
      >
        <Camera className="size-6" />
      </button>

      {/* Bottom Navigation */}
      <nav className="fixed bottom-0 left-0 right-0 bg-white border-t border-neutral-200 safe-area-inset-bottom z-40">
        <div className="flex items-center justify-around px-2 py-2">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive =
              location.pathname === item.path ||
              (item.path !== "/" && location.pathname.startsWith(item.path));

            return (
              <Link
                key={item.path}
                to={item.path}
                className={cn(
                  "flex flex-col items-center gap-1 py-2 px-4 rounded-xl transition-all min-w-[64px]",
                  isActive
                    ? "text-blue-600 bg-blue-50"
                    : "text-neutral-600 hover:bg-neutral-50"
                )}
              >
                <Icon className={cn("size-6", isActive && "fill-blue-100")} />
                <span className="text-xs font-medium">{item.label}</span>
              </Link>
            );
          })}
        </div>
      </nav>
    </div>
  );
}