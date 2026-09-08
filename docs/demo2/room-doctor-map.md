# Bản đồ phòng và bác sĩ cố định

Mỗi phòng hoạt động có đúng một bác sĩ thuộc phòng đó. Cùng bác sĩ được xếp hai ca 07:30–11:30 và 13:30–17:30 từ thứ Hai đến thứ Bảy.

| Phòng | Tài khoản | Phạm vi |
|---|---|---|
| INT-103 | `doctor.int1` | Nội khoa |
| INT-104 | `doctor.int2` | Nội khoa |
| SUR-101 | `doctor.sur1` | Ngoại khoa |
| SUR-102 | `doctor.sur2` | Ngoại khoa/thủ thuật |
| PED-102 | `doctor.ped` | Nhi khoa |
| DER-104 | `doctor.der` | Da liễu |
| OBG-107 | `doctor.obg` | Sản phụ khoa |
| LAB-201 | `doctor.lab201` | Huyết học, vi sinh, test nhanh |
| LAB-202 | `doctor.lab202` | Sinh hóa, nước tiểu |
| IMG-301 | `doctor.img301` | Siêu âm, điện tim |
| XR-302 | `doctor.xr302` | X-quang |

Trong ca mở cửa, INT-104 và SUR-102 được seed nhiều hơn phòng số 1 tương ứng bốn phiếu. Vì backend vẫn cân bằng tải thật, ba lượt mới đầu tiên sẽ vào INT-103 hoặc SUR-101. Sau đó nên chạy lại seed trước lần tập tiếp theo.

Lịch này tối ưu cho trình diễn, không được mô tả là mô hình phân ca lao động ngoài thực tế.
