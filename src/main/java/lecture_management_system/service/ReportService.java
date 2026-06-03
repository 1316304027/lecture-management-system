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
 * 【修正】公開課題のみを集計対象にするよう変更。
 *
 * ■ 修正前の問題
 *   - 提出率・課題数の分母に未公開課題も含まれていた
 *   - 平均スコアが「採点済み提出の平均」のみで、未提出課題が無視されていた
 *
 * ■ 修正後の仕様
 *   - 課題数（分母）：published=true の課題のみカウント
 *   - 提出数（分子）：公開課題への提出のみカウント
 *   - 提出率        ：公開課題数ベースで計算
 *   - 未提出数      ：公開課題数 − 公開課題への提出数
 *   - 平均スコア    ：公開課題数を分母とし、未提出・未採点は0点扱いで計算
 *                    （採点済み提出のスコア合計 ÷ 公開課題総数）
 */
@Service
public class ReportService {

    @Autowired private CourseUserRepository courseUserRepository;
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private CourseScheduleRepository courseScheduleRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private SubmissionRepository submissionRepository;

    public List<StudentReportDto> getCourseReport(Long courseId) {

        // 実施済み授業数（出席率の分母）
        long totalLessons = courseScheduleRepository
                .countByCourse_IdAndLessonDateLessThanEqual(courseId, LocalDate.now());

        // 【修正】公開課題数のみを分母にする（未公開は除外）
        long totalPublishedAssignments =
                assignmentRepository.countByCourse_IdAndPublishedTrue(courseId);

        return courseUserRepository.findByCourse_IdAndRole(courseId, "STUDENT")
                .stream()
                .map(cu -> {
                    User student = cu.getUser();
                    StudentReportDto dto = new StudentReportDto();
                    dto.setStudentId(student.getId());
                    dto.setStudentName(student.getName());
                    dto.setStudentEmail(student.getEmail());
                    dto.setTotalLessons(totalLessons);

                    // 【修正】課題数の分母も公開課題数にセット
                    dto.setTotalAssignments(totalPublishedAssignments);

                    // 出席回数
                    dto.setAttendedCount(
                            attendanceRepository.countByStudent_IdAndCourse_Id(
                                    student.getId(), courseId));

                    // 【修正】公開課題への提出数のみカウント
                    dto.setSubmittedCount(
                            submissionRepository
                                .countByStudent_IdAndAssignment_Course_IdAndAssignment_PublishedTrue(
                                        student.getId(), courseId));

                    // 【修正】平均スコア：未提出・未採点を0点扱い、公開課題数で割る
                    dto.setAverageScore(
                            calcAverageScore(student.getId(), courseId, totalPublishedAssignments));

                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 平均スコア計算
     *
     * 【修正後の仕様】
     *   分子：公開課題への採点済み提出スコアの合計
     *   分母：公開課題の総数（未提出・未採点は0点として扱う）
     *
     * 例）公開課題4件、提出1件（80点採点済み）、残り3件未提出の場合
     *   → (80 + 0 + 0 + 0) ÷ 4 = 20.0点
     *
     * 公開課題が0件の場合は null を返す（表示上「採点待ち」）。
     */
    private Double calcAverageScore(Long studentId, Long courseId, long totalPublishedAssignments) {
        if (totalPublishedAssignments == 0) return null;

        // 公開課題への提出のみ取得
        List<Submission> submissions = submissionRepository
                .findByStudent_IdAndAssignment_Course_IdAndAssignment_PublishedTrue(
                        studentId, courseId);

        // 採点済み提出のスコア合計
        int scoreSum = submissions.stream()
                .filter(s -> s.getScore() != null)
                .mapToInt(Submission::getScore)
                .sum();

        // 未提出・未採点は0点扱い → 公開課題総数で割る
        double avg = (double) scoreSum / totalPublishedAssignments;
        return Math.round(avg * 10) / 10.0;
    }
}