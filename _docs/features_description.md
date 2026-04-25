# Chức năng của Document File Editor App

Tài liệu này mô tả công dụng và cách hoạt động của các chức năng chính trong ứng dụng Document File Editor.

## 1. Xác thực (Authentication)
- **Login (Đăng nhập):** Sử dụng dịch vụ **Firebase Authentication** để xác thực thông tin đăng nhập của người dùng qua email và mật khẩu một cách bảo mật và nhanh chóng.
- **Register (Đăng ký):** Người dùng có thể tạo một tài khoản mới an toàn thông qua hệ thống **Firebase Authentication**.

## 2. Quản lý Tài liệu (Document Management)
Ứng dụng sử dụng mô hình lưu trữ kết hợp giữa Firebase và Cloudinary để quản lý dữ liệu tối ưu:

- **Upload File (Tải lên file):** 
  - **Khối dữ liệu (Physical File):** File thực tế của người dùng được tải lên và lưu trữ trên Cloud storage của hệ thống **Cloudinary**.
  - **Siêu dữ liệu (Metadata):** Các thông tin về file như Tên tài liệu, kích thước, định dạng, đường dẫn URL trả về từ Cloudinary, thông tin người dùng sở hữu sẽ được lưu vào cơ sở dữ liệu **Firebase (Firebase Realtime Database/Firestore)** để tải lượng dữ liệu một cách tối ưu.
- **View Documents (Xem tài liệu):** Ứng dụng sẽ gọi api truy xuất Metadata từ **Firebase** để hiển thị danh sách các tài liệu nhanh chóng mà không cần tải chi tiết từng file.
- **Download File (Tải file xuống):** Người dùng tải về file thông qua đường dẫn trực tiếp trỏ đến **Cloudinary** đã được lưu trong metadata.
- **Delete File (Xóa file):** Khi xóa, hệ thống sẽ gửi lệnh xoá file gốc trên **Cloudinary** và đồng thời xóa metadata tương ứng khỏi **Firebase**.
- **Create Missing Folder (Tạo thư mục nếu thiếu):** Ứng dụng tự động kiểm tra và tạo các thư mục (như Images, Docs, v.v.) bên trong thiết bị hoặc cây dữ liệu tài khoản nếu chưa tồn tại, đảm bảo cấu trúc tổ chức file không thay đổi.

## 3. Công cụ Tài liệu (Document Tools)
- **Scan Document (Quét tài liệu):** Sử dụng camera của thiết bị để quét tài liệu giấy, làm phẳng và chuyển đổi thành dạng kỹ thuật số (thường là PDF hoặc hình ảnh). Lưu trữ online thông qua tiến trình Upload File ở trên.
- **Translate Text (Dịch văn bản):** Dịch đoạn văn bản được chọn hoặc được trích xuất từ ngôn ngữ này sang ngôn ngữ khác.
- **OCR - Extract Text (Nhận dạng ký tự quang học):** Trích xuất văn bản từ hình ảnh hoặc tài liệu được quét, cho phép người dùng sao chép, chỉnh sửa văn bản từ ảnh.

## 4. Chỉnh sửa Hình ảnh (Image Editing)
- **Filter Image (Lọc hình ảnh):** Áp dụng các bộ lọc màu sắc hoặc hiệu ứng để thay đổi diện mạo của hình ảnh (ví dụ: đen trắng, sepia).
- **Rotate Image (Xoay hình ảnh):** Cho phép người dùng linh hoạt xoay hình ảnh theo mọi hướng (trái, phải, hay một góc tùy ý).
- **Crop Image (Cắt hình ảnh):** Loại bỏ các phần không mong muốn ở rìa của hình ảnh để tập trung vào đối tượng chính.
- **Enhance Image (Nâng cao hình ảnh):** Tự động hoặc thủ công điều chỉnh độ sáng, độ tương phản, độ sắc nét để cải thiện chất lượng hình ảnh.

## 5. Chỉnh sửa Tài liệu văn bản (Document Editing)
- **Edit Document (Chỉnh sửa tài liệu text):** Cung cấp các công cụ để chỉnh sửa nội dung văn bản (vd: .docx, .txt), định dạng văn bản (in đậm, in nghiêng, cỡ chữ, v.v.).
- **Edit Excel (Chỉnh sửa file Excel):** Cung cấp giao diện bảng tính cơ bản để xem và chỉnh sửa dữ liệu, công thức của file Excel (vd: .xlsx).

## 6. Công cụ PDF (PDF Tools)
- **Convert PDF (Chuyển đổi PDF):** Chuyển đổi qua lại giữa định dạng PDF và các định dạng khác (ví dụ: PDF sang Word, Image sang PDF).
- **Merge PDF (Gộp PDF):** Kết hợp nhiều file PDF riêng lẻ thành một file PDF duy nhất.
- **Split PDF (Chia nhỏ PDF):** Tách một file PDF lớn thành nhiều file PDF nhỏ hơn dựa trên số trang.
- **Annotate PDF (Chú thích PDF):** Thêm ghi chú, đánh dấu (highlight), vẽ tự do lên nội dung của file PDF.
