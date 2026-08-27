package org.example.doansummer2026.repository;
import org.example.doansummer2026.model.AttendanceQrToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.*;
public interface AttendanceQrTokenRepository extends JpaRepository<AttendanceQrToken,UUID> {
 Optional<AttendanceQrToken> findByTokenHashAndActiveTrue(String hash);
 @Modifying @Query("update AttendanceQrToken t set t.active=false where t.active=true")
 int deactivateAll();
}
