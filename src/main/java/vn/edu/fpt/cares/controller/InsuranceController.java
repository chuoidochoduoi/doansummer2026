package vn.edu.fpt.cares.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.dto.insurance.InsuranceResponse;
import vn.edu.fpt.cares.service.InsuranceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/insurances")
@RequiredArgsConstructor
@Tag(name = "Insurance", description = "Quản lý bảo hiểm y tế")
public class InsuranceController {

    private final InsuranceService insuranceService;

    @Operation(summary = "Lấy danh sách các loại bảo hiểm và quy tắc giảm giá")
    @GetMapping
    public List<InsuranceResponse> getAll() {
        return insuranceService.getAllInsurances();
    }
}
