import { useState } from "react";
import { useNavigate } from "react-router";
import {
  Search,
  Filter,
  Upload,
  MoreVertical,
  FileText,
  Sheet,
  File,
  Star,
  Trash2,
  Menu,
  Folder,
  FolderPlus,
  ChevronRight,
  Home,
  ArrowLeft,
  Plus,
  X,
  FolderOpen,
} from "lucide-react";
import { cn } from "../components/ui/utils";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "../components/ui/dropdown-menu";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "../components/ui/dialog";
import { toast } from "sonner";

type Document = {
  id: string;
  name: string;
  type: "document" | "sheet" | "pdf";
  size: string;
  modified: string;
  starred: boolean;
  folderId: string | null;
};

type FolderType = {
  id: string;
  name: string;
  color: string;
  icon: string;
  itemCount: number;
  parentId: string | null;
};

const mockFolders: FolderType[] = [
  {
    id: "work",
    name: "Công việc",
    color: "bg-blue-100 text-blue-600",
    icon: "💼",
    itemCount: 12,
    parentId: null,
  },
  {
    id: "personal",
    name: "Cá nhân",
    color: "bg-purple-100 text-purple-600",
    icon: "👤",
    itemCount: 8,
    parentId: null,
  },
  {
    id: "projects",
    name: "Dự án",
    color: "bg-green-100 text-green-600",
    icon: "📁",
    itemCount: 15,
    parentId: null,
  },
  {
    id: "archive",
    name: "Lưu trữ",
    color: "bg-amber-100 text-amber-600",
    icon: "📦",
    itemCount: 24,
    parentId: null,
  },
];

const mockDocuments: Document[] = [
  {
    id: "1",
    name: "Q4 Financial Report.pdf",
    type: "pdf",
    size: "2.4 MB",
    modified: "2 days ago",
    starred: true,
    folderId: "work",
  },
  {
    id: "2",
    name: "Project Proposal.docx",
    type: "document",
    size: "856 KB",
    modified: "5 days ago",
    starred: false,
    folderId: "work",
  },
  {
    id: "3",
    name: "Sales Data 2025.xlsx",
    type: "sheet",
    size: "1.2 MB",
    modified: "1 week ago",
    starred: true,
    folderId: "work",
  },
  {
    id: "4",
    name: "Meeting Notes.pdf",
    type: "pdf",
    size: "524 KB",
    modified: "2 weeks ago",
    starred: false,
    folderId: "personal",
  },
  {
    id: "5",
    name: "Budget Analysis.xlsx",
    type: "sheet",
    size: "978 KB",
    modified: "3 weeks ago",
    starred: false,
    folderId: "projects",
  },
  {
    id: "6",
    name: "Product Roadmap.docx",
    type: "document",
    size: "1.5 MB",
    modified: "1 month ago",
    starred: true,
    folderId: null,
  },
  {
    id: "7",
    name: "Contract Template.pdf",
    type: "pdf",
    size: "456 KB",
    modified: "1 month ago",
    starred: false,
    folderId: "archive",
  },
  {
    id: "8",
    name: "Employee List.xlsx",
    type: "sheet",
    size: "342 KB",
    modified: "2 months ago",
    starred: false,
    folderId: null,
  },
];

export function Documents() {
  const navigate = useNavigate();
  const [documents, setDocuments] = useState(mockDocuments);
  const [folders, setFolders] = useState(mockFolders);
  const [searchQuery, setSearchQuery] = useState("");
  const [currentFolderId, setCurrentFolderId] = useState<string | null>(null);
  const [showNewFolderDialog, setShowNewFolderDialog] = useState(false);
  const [newFolderName, setNewFolderName] = useState("");
  const [selectedFolderColor, setSelectedFolderColor] = useState("bg-blue-100 text-blue-600");

  const folderColors = [
    { color: "bg-blue-100 text-blue-600", name: "Blue" },
    { color: "bg-purple-100 text-purple-600", name: "Purple" },
    { color: "bg-green-100 text-green-600", name: "Green" },
    { color: "bg-red-100 text-red-600", name: "Red" },
    { color: "bg-amber-100 text-amber-600", name: "Amber" },
    { color: "bg-pink-100 text-pink-600", name: "Pink" },
  ];

  const currentFolder = folders.find((f) => f.id === currentFolderId);

  const filteredDocuments = documents.filter((doc) => {
    const matchesSearch = doc.name.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesFolder = doc.folderId === currentFolderId;
    return matchesSearch && matchesFolder;
  });

  const currentFolders = folders.filter((f) => f.parentId === currentFolderId);

  const getDocumentIcon = (type: string) => {
    switch (type) {
      case "document":
        return <FileText className="size-5" />;
      case "sheet":
        return <Sheet className="size-5" />;
      case "pdf":
        return <File className="size-5" />;
      default:
        return <FileText className="size-5" />;
    }
  };

  const getDocumentColor = (type: string) => {
    switch (type) {
      case "document":
        return "bg-blue-100 text-blue-600";
      case "sheet":
        return "bg-green-100 text-green-600";
      case "pdf":
        return "bg-red-100 text-red-600";
      default:
        return "bg-neutral-100 text-neutral-600";
    }
  };

  const toggleStar = (id: string) => {
    setDocuments(
      documents.map((doc) =>
        doc.id === id ? { ...doc, starred: !doc.starred } : doc
      )
    );
  };

  const handleDocumentClick = (doc: Document) => {
    if (doc.type === "sheet") {
      navigate(`/excel/${doc.id}`);
    } else {
      navigate(`/document/${doc.id}`);
    }
  };

  const handleFolderClick = (folderId: string) => {
    setCurrentFolderId(folderId);
  };

  const handleBackClick = () => {
    if (currentFolderId) {
      const folder = folders.find((f) => f.id === currentFolderId);
      setCurrentFolderId(folder?.parentId || null);
    }
  };

  const handleCreateFolder = () => {
    if (!newFolderName.trim()) {
      toast.error("Vui lòng nhập tên thư mục");
      return;
    }

    const newFolder: FolderType = {
      id: `folder-${Date.now()}`,
      name: newFolderName,
      color: selectedFolderColor,
      icon: "📁",
      itemCount: 0,
      parentId: currentFolderId,
    };

    setFolders([...folders, newFolder]);
    setNewFolderName("");
    setShowNewFolderDialog(false);
    toast.success("Đã tạo thư mục mới!");
  };

  const deleteFolder = (folderId: string) => {
    setFolders(folders.filter((f) => f.id !== folderId));
    toast.success("Đã xóa thư mục!");
  };

  return (
    <div className="h-full flex flex-col bg-neutral-50">
      {/* Header */}
      <div className="bg-white border-b border-neutral-200 px-4 py-3">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            {currentFolderId ? (
              <button
                onClick={handleBackClick}
                className="p-2 hover:bg-neutral-100 rounded-lg -ml-2"
              >
                <ArrowLeft className="size-5" />
              </button>
            ) : null}
            <h1 className="text-xl">
              {currentFolder ? currentFolder.name : "Tài liệu của tôi"}
            </h1>
          </div>
          <button className="p-2 hover:bg-neutral-100 rounded-lg">
            <Menu className="size-5" />
          </button>
        </div>

        {/* Breadcrumb */}
        {currentFolderId && (
          <div className="flex items-center gap-1 text-sm text-neutral-600 mb-3">
            <button
              onClick={() => setCurrentFolderId(null)}
              className="hover:text-blue-600 flex items-center gap-1"
            >
              <Home className="size-4" />
              Trang chủ
            </button>
            <ChevronRight className="size-4" />
            <span className="font-medium text-neutral-900">{currentFolder?.name}</span>
          </div>
        )}

        {/* Search Bar */}
        <div className="relative mb-3">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-neutral-400" />
          <Input
            placeholder="Tìm kiếm tài liệu..."
            className="pl-10 h-11"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        {/* Action Buttons */}
        <div className="flex gap-2">
          <Button
            variant="outline"
            className="flex-1 gap-2 h-11"
            onClick={() => setShowNewFolderDialog(true)}
          >
            <FolderPlus className="size-4" />
            Tạo thư mục
          </Button>
          <Button variant="outline" className="flex-1 gap-2 h-11">
            <Filter className="size-4" />
            Lọc
          </Button>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto pb-20">
        <div className="p-4 space-y-4">
          {/* Folders */}
          {currentFolders.length > 0 && (
            <div className="space-y-2">
              <h2 className="text-sm font-semibold text-neutral-600 px-1">
                Thư mục
              </h2>
              <div className="grid grid-cols-2 gap-3">
                {currentFolders.map((folder) => (
                  <button
                    key={folder.id}
                    onClick={() => handleFolderClick(folder.id)}
                    className="bg-white border border-neutral-200 rounded-xl p-4 text-left hover:border-blue-300 hover:shadow-sm transition-all active:scale-95 relative group"
                  >
                    <div className="flex items-start justify-between mb-2">
                      <div className={cn("p-2 rounded-lg text-2xl", folder.color)}>
                        <FolderOpen className="size-6" />
                      </div>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                            }}
                            className="p-1 hover:bg-neutral-100 rounded opacity-0 group-hover:opacity-100 transition-opacity"
                          >
                            <MoreVertical className="size-4 text-neutral-600" />
                          </button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem>Đổi tên</DropdownMenuItem>
                          <DropdownMenuItem>Di chuyển</DropdownMenuItem>
                          <DropdownMenuItem
                            className="text-red-600"
                            onClick={() => deleteFolder(folder.id)}
                          >
                            <Trash2 className="size-4 mr-2" />
                            Xóa
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                    <h3 className="font-medium text-sm mb-1 truncate">
                      {folder.name}
                    </h3>
                    <p className="text-xs text-neutral-500">
                      {folder.itemCount} mục
                    </p>
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Documents */}
          {filteredDocuments.length > 0 && (
            <div className="space-y-2">
              <h2 className="text-sm font-semibold text-neutral-600 px-1">
                Tài liệu
              </h2>
              {filteredDocuments.map((doc) => (
                <div
                  key={doc.id}
                  onClick={() => handleDocumentClick(doc)}
                  className="bg-white border border-neutral-200 rounded-xl p-4 active:bg-neutral-50 transition-colors"
                >
                  <div className="flex items-start gap-3">
                    <div
                      className={cn(
                        "p-3 rounded-lg shrink-0",
                        getDocumentColor(doc.type)
                      )}
                    >
                      {getDocumentIcon(doc.type)}
                    </div>
                    <div className="flex-1 min-w-0">
                      <h3 className="font-medium mb-1 truncate">{doc.name}</h3>
                      <p className="text-sm text-neutral-500">
                        {doc.size} • {doc.modified}
                      </p>
                    </div>
                    <div className="flex gap-1 shrink-0">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          toggleStar(doc.id);
                        }}
                        className="p-2 hover:bg-neutral-100 rounded-lg"
                      >
                        <Star
                          className={cn(
                            "size-5",
                            doc.starred
                              ? "fill-yellow-400 text-yellow-400"
                              : "text-neutral-400"
                          )}
                        />
                      </button>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <button
                            onClick={(e) => e.stopPropagation()}
                            className="p-2 hover:bg-neutral-100 rounded-lg"
                          >
                            <MoreVertical className="size-5 text-neutral-600" />
                          </button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem
                            onClick={() => handleDocumentClick(doc)}
                          >
                            Mở
                          </DropdownMenuItem>
                          <DropdownMenuItem>Tải xuống</DropdownMenuItem>
                          <DropdownMenuItem>Chia sẻ</DropdownMenuItem>
                          <DropdownMenuItem>Di chuyển đến</DropdownMenuItem>
                          <DropdownMenuItem className="text-red-600">
                            <Trash2 className="size-4 mr-2" />
                            Xóa
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Empty State */}
          {currentFolders.length === 0 && filteredDocuments.length === 0 && (
            <div className="flex flex-col items-center justify-center py-12 text-center">
              <div className="size-20 bg-neutral-100 rounded-full flex items-center justify-center mb-4">
                <Folder className="size-10 text-neutral-400" />
              </div>
              <h3 className="font-semibold mb-1">
                {searchQuery ? "Không tìm thấy kết quả" : "Thư mục trống"}
              </h3>
              <p className="text-sm text-neutral-600 mb-4">
                {searchQuery
                  ? "Thử tìm kiếm với từ khóa khác"
                  : "Tạo thư mục hoặc tải lên tài liệu"}
              </p>
              {!searchQuery && (
                <Button onClick={() => setShowNewFolderDialog(true)}>
                  <FolderPlus className="size-4 mr-2" />
                  Tạo thư mục mới
                </Button>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Floating Action Button */}
      <button className="fixed bottom-20 right-4 size-14 bg-blue-600 text-white rounded-full shadow-lg flex items-center justify-center active:scale-95 transition-transform z-10">
        <Upload className="size-6" />
      </button>

      {/* New Folder Dialog */}
      <Dialog open={showNewFolderDialog} onOpenChange={setShowNewFolderDialog}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>Tạo thư mục mới</DialogTitle>
            <DialogDescription>
              Nhập tên và chọn màu cho thư mục của bạn
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Tên thư mục</label>
              <Input
                placeholder="Nhập tên thư mục..."
                value={newFolderName}
                onChange={(e) => setNewFolderName(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    handleCreateFolder();
                  }
                }}
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Màu sắc</label>
              <div className="grid grid-cols-6 gap-2">
                {folderColors.map((color) => (
                  <button
                    key={color.color}
                    onClick={() => setSelectedFolderColor(color.color)}
                    className={cn(
                      "size-10 rounded-lg border-2 transition-all",
                      color.color,
                      selectedFolderColor === color.color
                        ? "border-neutral-900 scale-110"
                        : "border-transparent"
                    )}
                  >
                    <Folder className="size-5 mx-auto" />
                  </button>
                ))}
              </div>
            </div>
          </div>
          <div className="flex gap-2">
            <Button
              variant="outline"
              onClick={() => {
                setShowNewFolderDialog(false);
                setNewFolderName("");
              }}
              className="flex-1"
            >
              Hủy
            </Button>
            <Button onClick={handleCreateFolder} className="flex-1">
              Tạo thư mục
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
