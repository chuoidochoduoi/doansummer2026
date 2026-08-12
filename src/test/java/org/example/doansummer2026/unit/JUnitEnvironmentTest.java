package org.example.doansummer2026.unit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Kiểm tra tối thiểu để xác nhận môi trường JUnit 5 hoạt động.
 *
 * Test này là unit test thuần túy: không tải Spring Context,
 * không kết nối cơ sở dữ liệu và không phụ thuộc dịch vụ bên ngoài.
 */
class JUnitEnvironmentTest {

    @Test
    @DisplayName("JUnit 5 thực thi được phép cộng và kiểm tra kết quả")
    void testJUnit() {
        int a = 10;
        int b = 20;

        int result = a + b;

        assertEquals(30, result);
    }
}
