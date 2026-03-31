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

type Document = {
  id: string;
  name: string;
  type: "document" | "sheet" | "pdf";
  size: string;
  modified: string;
  starred: boolean;
};

const mockDocuments: Document[] = [
  {
    id: "1",
    name: "Q4 Financial Report.pdf",
    type: "pdf",
    size: "2.4 MB",
    modified: "2 days ago",
    starred: true,
  },
  {
    id: "2",
    name: "Project Proposal.docx",
    type: "document",
    size: "856 KB",
    modified: "5 days ago",
    starred: false,
  },
  {
    id: "3",
    name: "Sales Data 2025.xlsx",
    type: "sheet",
    size: "1.2 MB",
    modified: "1 week ago",
    starred: true,
  },
  {
    id: "4",
    name: "Meeting Notes.pdf",
    type: "pdf",
    size: "524 KB",
    modified: "2 weeks ago",
    starred: false,
  },
  {
    id: "5",
    name: "Budget Analysis.xlsx",
    type: "sheet",
    size: "978 KB",
    modified: "3 weeks ago",
    starred: false,
  },
  {
    id: "6",
    name: "Product Roadmap.docx",
    type: "document",
    size: "1.5 MB",
    modified: "1 month ago",
    starred: true,
  },
  {
    id: "7",
    name: "Contract Template.pdf",
    type: "pdf",
    size: "456 KB",
    modified: "1 month ago",
    starred: false,
  },
  {
    id: "8",
    name: "Employee List.xlsx",
    type: "sheet",
    size: "342 KB",
    modified: "2 months ago",
    starred: false,
  },
];

export function Documents() {
  const navigate = useNavigate();
  const [documents, setDocuments] = useState(mockDocuments);
  const [searchQuery, setSearchQuery] = useState("");

  const filteredDocuments = documents.filter((doc) =>
    doc.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

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

  return (
    <div className="h-full flex flex-col">
      {/* Header */}
      <div className="bg-white border-b border-neutral-200 px-4 py-3">
        <div className="flex items-center justify-between mb-3">
          <h1 className="text-xl">My Documents</h1>
          <button className="p-2 hover:bg-neutral-100 rounded-lg">
            <Menu className="size-5" />
          </button>
        </div>

        {/* Search Bar */}
        <div className="relative mb-3">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-neutral-400" />
          <Input
            placeholder="Search documents..."
            className="pl-10 h-11"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        {/* Filter Button */}
        <Button variant="outline" className="w-full gap-2 h-11">
          <Filter className="size-4" />
          Filter & Sort
        </Button>
      </div>

      {/* Documents List */}
      <div className="flex-1 overflow-auto">
        <div className="p-4 space-y-3">
          {filteredDocuments.map((doc) => (
            <div
              key={doc.id}
              onClick={() => navigate(`/document/${doc.id}`)}
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
                        onClick={() => navigate(`/document/${doc.id}`)}
                      >
                        Open
                      </DropdownMenuItem>
                      <DropdownMenuItem>Download</DropdownMenuItem>
                      <DropdownMenuItem>Share</DropdownMenuItem>
                      <DropdownMenuItem className="text-red-600">
                        <Trash2 className="size-4 mr-2" />
                        Delete
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Floating Action Button */}
      <button className="fixed bottom-20 right-4 size-14 bg-blue-600 text-white rounded-full shadow-lg flex items-center justify-center active:scale-95 transition-transform">
        <Upload className="size-6" />
      </button>
    </div>
  );
}