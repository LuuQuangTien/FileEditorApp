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
import { ExcelEditor } from "./pages/ExcelEditor";
import { CropImage } from "./pages/CropImage";
import { RotateImage } from "./pages/RotateImage";
import { ImageFilter } from "./pages/ImageFilter";
import { EnhanceImage } from "./pages/EnhanceImage";

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
      { path: "tool/crop-image", Component: CropImage },
      { path: "tool/rotate-image", Component: RotateImage },
      { path: "tool/image-filter", Component: ImageFilter },
      { path: "tool/enhance-image", Component: EnhanceImage },
      { path: "tool/:toolId", Component: ToolPage },
    ],
  },
  {
    path: "/document/:id",
    Component: DocumentEditor,
  },
  {
    path: "/excel/:id",
    Component: ExcelEditor,
  },
]);