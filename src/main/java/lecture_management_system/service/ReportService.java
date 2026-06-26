package lecture_management_system.service;

import lecture_management_system.dto.StudentReportDto;
import lecture_management_system.dto.LessonAttendanceStatDto;
import lecture_management_system.dto.AssignmentAvgScoreDto;
import lecture_management_system.dto.StudentAttendancePreviewRow;
import lecture_management_system.dto.StudentAssignmentPreviewRow;
import lecture_management_system.entity.Submission;
import lecture_management_system.entity.User;
import lecture_management_system.entity.Assignment;
import lecture_management_system.entity.CourseSchedule;
import lecture_management_system.entity.Attendance;
import lecture_management_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 実績レポート業務Service
 *
 * 平均スコア：採点済み提出のみの平均（未採点は0点扱いしない → 「採点待ち」表示）
 */
@Service
public class ReportService {

    @Autowired private CourseUserRepository courseUserRepository;
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private CourseScheduleRepository courseScheduleRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private SubmissionRepository submissionRepository;

    public List<StudentReportDto> getCourseReport(Long courseId) {

        long totalLessons = courseScheduleRepository
                .countByCourse_IdAndLessonDateLessThanEqual(courseId, LocalDate.now());

        long totalPublishedAssignments =
                assignmentRepository.countByCourse_IdAndPublishedTrue(courseId);

        return courseUserRepository.findByCourse_IdAndRole(courseId, "STUDENT")
                .stream()
                .map(cu -> {
                    User student = cu.getUser();
                    Long studentId = student.getId();

                    StudentReportDto dto = new StudentReportDto();
                    dto.setStudentId(studentId);
                    dto.setStudentName(student.getName());
                    dto.setStudentEmail(student.getEmail());
                    dto.setTotalLessons(totalLessons);
                    dto.setTotalAssignments(totalPublishedAssignments);

                    dto.setAttendedCount(
                            attendanceRepository.countByStudent_IdAndCourse_Id(studentId, courseId));

                    dto.setSubmittedCount(
                            submissionRepository
                                .countByStudent_IdAndAssignment_Course_IdAndAssignment_PublishedTrue(
                                        studentId, courseId));

                    dto.setEvaluatedCount(
                            submissionRepository
                                .countByStudent_IdAndAssignment_Course_IdAndAssignment_PublishedTrueAndScoreIsNotNull(
                                        studentId, courseId));

                    dto.setPendingGradeCount(
                            submissionRepository
                                .countByStudent_IdAndAssignment_Course_IdAndAssignment_PublishedTrueAndScoreIsNull(
                                        studentId, courseId));

                    dto.setAverageScore(calcGradedAverage(studentId, courseId));

                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 採点済み提出のみの平均。1件も採点済みがなければ null（画面で「採点待ち」）。
     */
    private Double calcGradedAverage(Long studentId, Long courseId) {
        List<Submission> submissions = submissionRepository
                .findByStudent_IdAndAssignment_Course_IdAndAssignment_PublishedTrue(
                        studentId, courseId);

        List<Integer> scores = submissions.stream()
                .filter(s -> s.getScore() != null)
                .map(Submission::getScore)
                .toList();

        if (scores.isEmpty()) return null;

        double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
        return Math.round(avg * 10) / 10.0;
    }

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    public static String formatLessonTime(LocalTime start, LocalTime end) {
        if (start == null && end == null) return "時間未設定";
        if (start != null && end != null) {
            return start.toString().substring(0, 5) + "〜" + end.toString().substring(0, 5);
        }
        if (start != null) return start.toString().substring(0, 5) + "〜";
        return "〜" + end.toString().substring(0, 5);
    }

    /** 実施済み授業日ごとのコース出席率 */
    public List<LessonAttendanceStatDto> getLessonAttendanceStats(Long courseId) {
        long enrolled = courseUserRepository.findByCourse_IdAndRole(courseId, "STUDENT").size();
        LocalDate today = LocalDate.now();
        return courseScheduleRepository.findByCourse_IdOrderByLessonDateAsc(courseId).stream()
                .filter(s -> !s.getLessonDate().isAfter(today))
                .map(s -> {
                    LessonAttendanceStatDto dto = new LessonAttendanceStatDto();
                    dto.setLessonDate(s.getLessonDate());
                    dto.setStartTime(s.getStartTime());
                    dto.setEndTime(s.getEndTime());
                    dto.setLessonTimeLabel(formatLessonTime(s.getStartTime(), s.getEndTime()));
                    dto.setEnrolledCount(enrolled);
                    dto.setPresentCount(attendanceRepository.countByCourse_IdAndDate(courseId, s.getLessonDate()));
                    dto.calcRate();
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /** 受講者1名：授業日ごとの出席明細 */
    public List<StudentAttendancePreviewRow> getStudentAttendancePreview(Long courseId, Long studentId) {
        LocalDate today = LocalDate.now();
        List<CourseSchedule> schedules = courseScheduleRepository.findByCourse_IdOrderByLessonDateAsc(courseId);
        return schedules.stream().map(s -> {
            StudentAttendancePreviewRow row = new StudentAttendancePreviewRow();
            row.setLessonDate(s.getLessonDate());
            row.setLessonTimeLabel(formatLessonTime(s.getStartTime(), s.getEndTime()));
            row.setFutureLesson(s.getLessonDate().isAfter(today));
            if (row.isFutureLesson()) {
                row.setStatus("未実施");
            } else {
                Optional<Attendance> att = attendanceRepository.findByStudent_IdAndCourse_IdAndDate(
                        studentId, courseId, s.getLessonDate());
                if (att.isPresent()) {
                    row.setStatus("出席");
                    row.setCheckInAt(att.get().getAttendedAt());
                } else {
                    row.setStatus("欠席");
                }
            }
            return row;
        }).collect(Collectors.toList());
    }

    /** 受講者1名：公開課題ごとの提出・採点明細 */
    public List<StudentAssignmentPreviewRow> getStudentAssignmentPreview(Long courseId, Long studentId) {
        return assignmentRepository.findByCourse_IdAndPublishedTrue(courseId).stream().map(a -> {
            StudentAssignmentPreviewRow row = new StudentAssignmentPreviewRow();
            row.setTitle(a.getTitle());
            row.setDeadline(a.getDeadline());
            Submission sub = submissionRepository
                    .findByStudent_IdAndAssignment_Id(studentId, a.getId()).orElse(null);
            if (sub == null) {
                row.setStatus("未提出");
            } else if (sub.getScore() != null) {
                row.setStatus("採点済");
                row.setScore(sub.getScore());
            } else {
                row.setStatus("提出済（採点待ち）");
            }
            if (sub != null) row.setSubmittedAt(sub.getSubmittedAt());
            return row;
        }).collect(Collectors.toList());
    }

    /** 公開課題ごとの平均点（採点済みのみ） */
    public List<AssignmentAvgScoreDto> getAssignmentAvgScores(Long courseId) {
        return assignmentRepository.findByCourse_IdAndPublishedTrue(courseId).stream()
                .map(a -> {
                    AssignmentAvgScoreDto dto = new AssignmentAvgScoreDto();
                    dto.setAssignmentId(a.getId());
                    dto.setTitle(a.getTitle());
                    List<Submission> subs = submissionRepository.findByAssignment_IdOrderBySubmittedAtDesc(a.getId());
                    dto.setSubmittedCount(subs.size());
                    List<Integer> scores = subs.stream()
                            .filter(s -> s.getScore() != null)
                            .map(Submission::getScore)
                            .toList();
                    dto.setGradedCount(scores.size());
                    if (scores.isEmpty()) {
                        dto.setAverageScore(null);
                    } else {
                        double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
                        dto.setAverageScore(Math.round(avg * 10) / 10.0);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
