package vn.edu.fpt.cares.service;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.icd.ICD10CreateRequest;
import vn.edu.fpt.cares.dto.icd.ICD10UpdateRequest;
import vn.edu.fpt.cares.exception.ConflictException;
import vn.edu.fpt.cares.exception.ResourceNotFoundException;
import vn.edu.fpt.cares.model.Icd10Code;
import vn.edu.fpt.cares.repository.Icd10CodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Icd10CodeServiceTest {
    private Icd10CodeRepository repository;
    private Icd10CodeService service;

    @BeforeEach
    void setUp() {
        repository = mock(Icd10CodeRepository.class);
        service = new Icd10CodeService(repository);
    }

    @Test
    void searchAndGetMapRepositoryData() {
        var pageable = PageRequest.of(0, 10);
        var code = entity("J00", "Viêm mũi họng cấp", "Mô tả", "Hô hấp");
        when(repository.search("mũi", "Hô hấp", pageable))
                .thenReturn(new PageImpl<>(List.of(code), pageable, 1));
        when(repository.findById("J00")).thenReturn(Optional.of(code));

        PageResponse<?> page = service.search("mũi", "Hô hấp", pageable);

        assertEquals(1, page.totalElements());
        assertEquals("J00", service.get("J00").code());
    }

    @Test
    void createRejectsDuplicateAndPersistsNewCode() {
        var request = new ICD10CreateRequest("I10", "Tăng huyết áp", "Nguyên phát", "Tim mạch");
        when(repository.existsByCode("I10")).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.create(request));

        when(repository.existsByCode("I10")).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var created = service.create(request);

        assertAll(
                () -> assertEquals("I10", created.code()),
                () -> assertEquals("Tăng huyết áp", created.name()),
                () -> assertEquals("Nguyên phát", created.description()),
                () -> assertEquals("Tim mạch", created.category())
        );
    }

    @Test
    void updateChangesOnlyFieldsThatWereProvided() {
        var code = entity("E11", "Tên cũ", "Mô tả cũ", "Nội tiết");
        when(repository.findById("E11")).thenReturn(Optional.of(code));
        when(repository.save(code)).thenReturn(code);

        service.update("E11", new ICD10UpdateRequest("Tên mới", null, "Chuyển hóa"));
        assertEquals("Tên mới", code.getName());
        assertEquals("Mô tả cũ", code.getDescription());
        assertEquals("Chuyển hóa", code.getCategory());

        service.update("E11", new ICD10UpdateRequest(null, "Mô tả mới", null));
        assertEquals("Tên mới", code.getName());
        assertEquals("Mô tả mới", code.getDescription());
        assertEquals("Chuyển hóa", code.getCategory());
    }

    @Test
    void missingCodesAreRejectedAndDeleteOnlyRemovesExistingCode() {
        when(repository.findById("X00")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.get("X00"));
        assertThrows(ResourceNotFoundException.class,
                () -> service.update("X00", new ICD10UpdateRequest(null, null, null)));

        when(repository.existsById("X00")).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.delete("X00"));
        verify(repository, never()).deleteById("X00");

        when(repository.existsById("J00")).thenReturn(true);
        service.delete("J00");
        verify(repository).deleteById("J00");
    }

    private Icd10Code entity(String code, String name, String description, String category) {
        return Icd10Code.builder().code(code).name(name).description(description).category(category).build();
    }
}
