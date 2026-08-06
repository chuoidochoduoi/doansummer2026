package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.dto.attendance.*;
import org.example.doansummer2026.enums.*;
import org.example.doansummer2026.exception.*;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class AttendanceService {
 private static final String PREFIX="ATTENDANCE:";
 private final StaffScheduleRepository schedules; private final StaffAttendanceRepository attendances;
 private final AttendanceQrTokenRepository tokens; private final AttendanceAdjustmentRepository adjustments;
 private final StaffInfoRepository staffRepo;
 @Value("${app.attendance.qr-valid-seconds:30}") private long qrSeconds;
 @Value("${app.attendance.allowed-ip-prefix:}") private String allowedIpPrefix;

 @Transactional public QrTokenResponse issueToken(UUID managerId){
  tokens.deactivateAll();
  String raw=UUID.randomUUID()+"."+UUID.randomUUID(); LocalDateTime expiry=LocalDateTime.now().plusSeconds(qrSeconds);
  tokens.save(AttendanceQrToken.builder().tokenHash(hash(raw)).expiresAt(expiry).createdBy(staff(managerId)).active(true).build());
  return new QrTokenResponse(PREFIX+raw,expiry,qrSeconds);
 }
 @Transactional public AttendanceResponse scan(UUID staffId,String value,String ip,String agent){
  if(value==null||!value.startsWith(PREFIX)) throw new BadRequestException("Ma QR diem danh khong hop le");
  if(allowedIpPrefix!=null&&!allowedIpPrefix.isBlank()&&(ip==null||!ip.startsWith(allowedIpPrefix.trim())))
   throw new BadRequestException("Chi duoc diem danh khi ket noi mang cua phong kham");
  AttendanceQrToken token=tokens.findByTokenHashAndActiveTrue(hash(value.substring(PREFIX.length())))
   .orElseThrow(()->new BadRequestException("Ma QR diem danh khong hop le"));
  LocalDateTime now=LocalDateTime.now();
  if(token.getExpiresAt().isBefore(now)){token.setActive(false);throw new BadRequestException("Ma QR da het han, vui long quet ma moi");}
  StaffSchedule schedule=currentSchedule(staffId,now); LocalDateTime start=LocalDateTime.of(schedule.getWorkDate(),start(schedule.getShift()));
  LocalDateTime end=LocalDateTime.of(schedule.getWorkDate(),end(schedule.getShift()));
  StaffAttendance a=attendances.findBySchedule_ScheduleId(schedule.getScheduleId()).orElse(null);
  if(a==null) a=StaffAttendance.builder().schedule(schedule).staff(staff(staffId)).checkInAt(now).checkInIp(ip)
   .deviceInfo(limit(agent)).status(now.isAfter(start.plusMinutes(5))?AttendanceStatus.LATE:AttendanceStatus.ON_TIME).build();
  else if(a.getCheckInAt()==null){a.setCheckInAt(now);a.setCheckInIp(ip);a.setDeviceInfo(limit(agent));a.setStatus(now.isAfter(start.plusMinutes(5))?AttendanceStatus.LATE:AttendanceStatus.ON_TIME);}
  else if(a.getCheckOutAt()==null){a.setCheckOutAt(now);a.setCheckOutIp(ip);a.setStatus(now.isBefore(end.minusMinutes(15))?AttendanceStatus.LEFT_EARLY:AttendanceStatus.COMPLETED);schedule.setStatus(ScheduleStatus.COMPLETED);}
  else throw new BadRequestException("Ca lam viec nay da check-in va check-out day du");
  return AttendanceResponse.from(attendances.save(a));
 }
 @Transactional(readOnly=true) public List<AttendanceTodayResponse> today(UUID staffId){return schedules.findAllByStaff_StaffIdAndWorkDate(staffId,LocalDate.now()).stream()
  .sorted(Comparator.comparing(s->start(s.getShift()))).map(s->{StaffAttendance a=attendances.findBySchedule_ScheduleId(s.getScheduleId()).orElse(null);
   return new AttendanceTodayResponse(s.getScheduleId(),s.getWorkDate(),s.getShift().getName(),start(s.getShift()),end(s.getShift()),a==null?null:a.getAttendanceId(),status(s,a),a==null?null:a.getCheckInAt(),a==null?null:a.getCheckOutAt());}).toList();}
 @Transactional(readOnly=true) public List<AttendanceManagementResponse> manage(LocalDate date){return schedules.findAllByWorkDateBetween(date,date).stream()
  .sorted(Comparator.comparing((StaffSchedule s)->name(s.getStaff())).thenComparing(s->start(s.getShift()))).map(s->{StaffAttendance a=attendances.findBySchedule_ScheduleId(s.getScheduleId()).orElse(null);
   return new AttendanceManagementResponse(s.getScheduleId(),s.getStaff().getStaffId(),s.getStaff().getStaffCode(),name(s.getStaff()),s.getWorkDate(),s.getShift().getName(),status(s,a),a==null?null:a.getCheckInAt(),a==null?null:a.getCheckOutAt());}).toList();}
 @Transactional public AdjustmentResponse request(UUID staffId,AdjustmentRequest req){
  StaffSchedule s=schedules.findById(req.scheduleId()).orElseThrow(()->new ResourceNotFoundException("Khong tim thay ca lam viec"));
  if(!s.getStaff().getStaffId().equals(staffId))throw new BadRequestException("Ca lam viec khong thuoc nhan vien hien tai");
  if(req.requestedCheckIn()==null&&req.requestedCheckOut()==null)throw new BadRequestException("Can nhap gio check-in hoac check-out de de nghi dieu chinh");
  StaffAttendance a=attendances.findBySchedule_ScheduleId(s.getScheduleId()).orElseGet(()->attendances.save(StaffAttendance.builder().schedule(s).staff(s.getStaff()).status(AttendanceStatus.ADJUSTMENT_PENDING).build()));a.setStatus(AttendanceStatus.ADJUSTMENT_PENDING);
  return AdjustmentResponse.from(adjustments.save(AttendanceAdjustment.builder().attendance(a).reason(req.reason().trim()).requestedCheckIn(req.requestedCheckIn()).requestedCheckOut(req.requestedCheckOut()).status(AttendanceAdjustmentStatus.PENDING).build()));
 }
 @Transactional(readOnly=true) public List<AdjustmentResponse> pending(){return adjustments.findAllByStatusOrderByCreatedAtAsc(AttendanceAdjustmentStatus.PENDING).stream().map(AdjustmentResponse::from).toList();}
 @Transactional public AdjustmentResponse review(UUID id,UUID managerId,boolean approved,String note){
  AttendanceAdjustment x=adjustments.findById(id).orElseThrow(()->new ResourceNotFoundException("Khong tim thay de nghi dieu chinh"));
  if(x.getStatus()!=AttendanceAdjustmentStatus.PENDING)throw new BadRequestException("De nghi nay da duoc xu ly");
  x.setStatus(approved?AttendanceAdjustmentStatus.APPROVED:AttendanceAdjustmentStatus.REJECTED);x.setReviewedBy(staff(managerId));x.setReviewedAt(LocalDateTime.now());x.setReviewNote(note);
  StaffAttendance a=x.getAttendance();if(approved){if(x.getRequestedCheckIn()!=null)a.setCheckInAt(x.getRequestedCheckIn());if(x.getRequestedCheckOut()!=null)a.setCheckOutAt(x.getRequestedCheckOut());}
  a.setStatus(a.getCheckInAt()==null?AttendanceStatus.ABSENT:a.getCheckOutAt()==null?AttendanceStatus.WORKING:AttendanceStatus.COMPLETED);return AdjustmentResponse.from(adjustments.save(x));
 }
 private StaffSchedule currentSchedule(UUID staffId,LocalDateTime now){return schedules.findAllByStaff_StaffIdAndWorkDate(staffId,now.toLocalDate()).stream().filter(s->s.getStatus()==ScheduleStatus.SCHEDULED)
  .filter(s->!now.isBefore(LocalDateTime.of(s.getWorkDate(),start(s.getShift())).minusMinutes(30))).filter(s->!now.isAfter(LocalDateTime.of(s.getWorkDate(),end(s.getShift())).plusMinutes(60)))
  .min(Comparator.comparingLong(s->Math.abs(Duration.between(now,LocalDateTime.of(s.getWorkDate(),start(s.getShift()))).toMinutes()))).orElseThrow(()->new BadRequestException("Khong co ca lam viec phu hop de diem danh luc nay"));}
 private String status(StaffSchedule s,StaffAttendance a){if(a!=null)return a.getStatus().name();return LocalDateTime.now().isAfter(LocalDateTime.of(s.getWorkDate(),end(s.getShift())).plusMinutes(60))?AttendanceStatus.ABSENT.name():"NOT_CHECKED_IN";}
 private StaffInfo staff(UUID id){return staffRepo.findById(id).orElseThrow(()->new ResourceNotFoundException("Khong tim thay nhan vien"));}
 private String name(StaffInfo s){return s.getProfile()!=null&&s.getProfile().getFullName()!=null?s.getProfile().getFullName():s.getStaffCode();}
 private LocalTime start(ShiftConfig s){return LocalTime.parse(s.getStartTime());}
 private LocalTime end(ShiftConfig s){return LocalTime.parse(s.getEndTime());}
 private String limit(String s){return s==null?null:s.substring(0,Math.min(500,s.length()));}
 private String hash(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
