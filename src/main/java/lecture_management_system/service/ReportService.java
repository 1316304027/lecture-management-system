package lecture_management_system.service;

import lecture_management_system.dto.StudentReportDto;
import lecture_management_system.entity.Submission;
import lecture_management_system.entity.User;
import lecture_management_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
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
}
