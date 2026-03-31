import { createBrowserRouter } from "react-router";
import { Layout } from "./components/Layout";
import { Home } from "./pages/Home";
import { Documents } from "./pages/Documents";
import { Tools } from "./pages/Tools";
import { Profile } from "./pages/Profile";
import { OCR } from "./pages/OCR";
import { Chat } from "./pages/Chat";
import { Scan } from "./pages/Scan";
import { Translate } from "./pages/Translate";
import { DocumentEditor } from "./pages/DocumentEditor";
import { ToolPage } from "./pages/ToolPage";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: Layout,
    children: [
      { index: true, Component: Home },
      { path: "files", Component: Documents },
      { path: "tools", Component: Tools },
      { path: "profile", Component: Profile },
      { path: "ocr", Component: OCR },
      { path: "chat", Component: Chat },
      { path: "scan", Component: Scan },
      { path: "translate", Component: Translate },
      { path: "tool/:toolId", Component: ToolPage },
    ],
  },
  {
    path: "/document/:id",
    Component: DocumentEditor,
  },
]);