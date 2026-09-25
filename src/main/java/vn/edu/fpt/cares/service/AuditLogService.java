package vn.edu.fpt.cares.service;

import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.auditlog.AuditLogCreateRequest;
import vn.edu.fpt.cares.dto.auditlog.AuditLogResponse;
import vn.edu.fpt.cares.enums.AuditAction;
import vn.edu.fpt.cares.model.AuditLog;
import vn.edu.fpt.cares.model.Account;
import vn.edu.fpt.cares.model.Profile;
import vn.edu.fpt.cares.repository.AuditLogRepository;
import vn.edu.fpt.cares.repository.AccountRepository;
import vn.edu.fpt.cares.repository.ProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.edu.fpt.cares.service.interfaces.AuditLogServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AuditLogService implements AuditLogServiceInterface {

    private final AuditLogRepository repo;
    private final AccountRepository accountRepo;
    private final ProfileRepository profileRepo;

    private String getActorName(UUID accountId) {
        if (accountId == null) return null;
        return profileRepo.findFirstByAccount_AccountId(accountId)
                .map(Profile::getFullName)
                .orElseGet(() -> accountRepo.findById(accountId)
                        .map(Account::getUsername)
                        .orElse(null));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(UUID actorId, AuditAction action, String entityName,
                                                   LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Page<AuditLog> page = repo.search(actorId, action, entityName, from, to, pageable);
        return PageResponse.from(page, a -> AuditLogResponse.from(a, getActorName(a.getActorAccountId())));
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> findByEntity(String entityName, String entityId) {
        return repo.findByEntityNameAndEntityIdOrderByCreatedAtDesc(entityName, entityId)
                .stream().map(a -> AuditLogResponse.from(a, getActorName(a.getActorAccountId()))).toList();
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public AuditLogResponse create(AuditLogCreateRequest req) {
        AuditLog a = AuditLog.builder()
                .action(req.action())
                .entityName(req.entityName())
                .entityId(req.entityId())
                .actorAccountId(req.actorAccountId())
                .ipAddress(req.ipAddress())
                .userAgent(req.userAgent())
                .oldValueJson(req.oldValueJson())
                .newValueJson(req.newValueJson())
                .description(req.description())
                .createdAt(LocalDateTime.now())
                .build();
        return AuditLogResponse.from(repo.save(a));
    }
}




