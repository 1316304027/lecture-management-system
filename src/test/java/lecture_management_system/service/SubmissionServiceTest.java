package lecture_management_system.service;

import lecture_management_system.entity.*;
import lecture_management_system.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * SubmissionService 単体テスト
 *
 * テスト観点：
 *  UT-SUB-01 正常系：期限内のPDFを正常に提出できる
 *  UT-SUB-02 異常系：期限切れは "deadline" を返す
 *  UT-SUB-03 異常系：同じ課題に2回提出すると "already" を返す
 *  UT-SUB-04 異常系：空ファイルは "empty" を返す
 *  UT-SUB-05 異常系：PDF以外のファイルは "pdf_only" を返す
 *  UT-SUB-06 正常系：講師が評価コメントとスコアを保存できる
 *  UT-SUB-07 正常系：スコアが0〜100の範囲にクランプされる（境界値）
 *  UT-SUB-08 正常系：平均スコアが正しく計算される
 *  UT-SUB-09 境界値：採点済み提出がない場合の平均スコアはnull
 */
@SpringBootTest
@Transactional
@DisplayName("SubmissionService 単体テスト")
class SubmissionServiceTest {

    @Autowired private SubmissionService submissionService;
    @Autowired private AssignmentService assignmentService;
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private SubmissionRepository submissionRepository;

    private User student;
    private Course course;
    private Assignment assignment;

    @BeforeEach
    void setUp() throws Exception {
        student = new User();
        student.setName("提出テスト学生");
        student.setEmail("sub_student@example.com");
        student.setPassword("dummy");
        student.setRole("STUDENT");
        student.setActive(true);
        student.setLocked(false);
        student.setLoginFailureCount(0);
        userRepository.save(student);

        course = new Course();
        course.setName("提出テストコース");
        courseRepository.save(course);

        // 期限内の課題
        assignment = new Assignment();
        assignment.setCourse(course);
        assignment.setTitle("提出テスト課題");
        assignment.setPublished(true);
        assignment.setDeadline(LocalDateTime.now().plusDays(7));
        assignment.setStoredFileName("");
        assignmentRepository.save(assignment);
    }

    // ----------------------------------------------------------------
    // UT-SUB-01: 正常な提出
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-SUB-01: 期限内のPDFを正常に提出すると success が返る")
    void test_submit_success() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "PDF content".getBytes());

        String result = submissionService.submit(student, assignment, pdf);

        assertThat(result).isEqualTo("success");
        assertThat(submissionRepository.findByStudent_IdAndAssignment_Id(
                student.getId(), assignment.getId())).isPresent();
    }

    // ----------------------------------------------------------------
    // UT-SUB-02: 期限切れ
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-SUB-02: 期限切れの課題に提出すると deadline が返る")
    void test_submit_deadline() throws Exception {
        Assignment expired = new Assignment();
        expired.setCourse(course);
        expired.setTitle("期限切れ課題");
        expired.setPublished(true);
        expired.setDeadline(LocalDateTime.now().minusHours(1));
        expired.setStoredFileName("");
        assignmentRepository.save(expired);

        MockMultipartFile pdf = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "content".getBytes());

        String result = submissionService.submit(student, expired, pdf);

        assertThat(result).isEqualTo("deadline");
    }

    // ----------------------------------------------------------------
    // UT-SUB-03: 重複提出
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-SUB-03: 同じ課題に2回提出すると already が返る")
    void test_submit_alreadySubmitted() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "content".getBytes());

        submissionService.submit(student, assignment, pdf);

        MockMultipartFile pdf2 = new MockMultipartFile(
                "file", "report2.pdf", "application/pdf", "content2".getBytes());
        String result = submissionService.submit(student, assignment, pdf2);

        assertThat(result).isEqualTo("already");
    }

    // ----------------------------------------------------------------
    // UT-SUB-04: 空ファイル
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-SUB-04: 空のファイルを提出すると empty が返る")
    void test_submit_emptyFile() throws Exception {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        String result = submissionService.submit(student, assignment, empty);

        assertThat(result).isEqualTo("empty");
    }

    // ----------------------------------------------------------------
    // UT-SUB-05: PDF以外のファイル
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-SUB-05: PDF以外のファイルを提出すると pdf_only が返る")
    void test_submit_notPdf() throws Exception {
        MockMultipartFile txt = new MockMultipartFile(
                "file", "report.txt", "text/plain", "not pdf".getBytes());

        String result = submissionService.submit(student, assignment, txt);

        assertThat(result).isEqualTo("pdf_only");
    }

    // ----------------------------------------------------------------
    // UT-SUB-06: 講師が評価コメントとスコアを保存できる
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-SUB-06: 講師がコメントとスコアを保存できる")
    void test_saveEvaluation() throws Exception {
        // 提出物を直接DBに作成
        Submission sub = new Submission();
        sub.setStudent(student);
        sub.setAssignment(assignment);
        sub.setStoredFileName("test.pdf");
        sub.setOriginalFileName("test.pdf");
        sub.setSubmittedAt(LocalDateTime.now());
        submissionRepository.save(sub);

        submissionService.saveEvaluation(sub.getId(), "よく書けています", 85);

        Submission updated = submissionRepository.findById(sub.getId()).orElseThrow();
        assertThat(updated.getEvaluationComment()).isEqualTo("よく書けています");
        assertThat(updated.getScore()).isEqualTo(85);
        assertThat(updated.getEvaluatedAt()).isNotNull();
    }

    // ----------------------------------------------------------------
    // UT-SUB-07: スコアのクランプ（境界値）
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-SUB-07: スコアが101以上の場合は100にクランプされる（境界値）")
    void test_saveEvaluation_scoreClamp() throws Exception {
        Submission sub = new Submission();
        sub.setStudent(student);
        sub.setAssignment(assignment);
        sub.setStoredFileName("test.pdf");
        sub.setOriginalFileName("test.pdf");
        sub.setSubmittedAt(LocalDateTime.now());
        submissionRepository.save(sub);

        submissionService.saveEvaluation(sub.getId(), null, 150);

        Submission updated = submissionRepository.findById(sub.getId()).orElseThrow();
        assertThat(updated.getScore()).isEqualTo(100);
    }

    // ----------------------------------------------------------------
    // UT-SUB-08: 平均スコア計算
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-SUB-08: 採点済み提出の平均スコアが正しく計算される（80+60=70.0）")
    void test_getAverageScore() throws Exception {
        // 課題を2つ作成して提出・採点
        for (int i = 0; i < 2; i++) {
            Assignment a = new Assignment();
            a.setCourse(course);
            a.setTitle("スコア課題" + i);
            a.setPublished(true);
            a.setDeadline(LocalDateTime.now().plusDays(7));
            a.setStoredFileName("");
            assignmentRepository.save(a);

            Submission sub = new Submission();
            sub.setStudent(student);
            sub.setAssignment(a);
            sub.setStoredFileName("f.pdf");
            sub.setOriginalFileName("f.pdf");
            sub.setSubmittedAt(LocalDateTime.now());
            sub.setScore(i == 0 ? 80 : 60);
            submissionRepository.save(sub);
        }

        Double avg = submissionService.getAverageScore(student.getId(), course.getId());

        assertThat(avg).isEqualTo(70.0);
    }

    // ----------------------------------------------------------------
    // UT-SUB-09: 採点済み提出がない場合は null（境界値）
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-SUB-09: 採点済み提出がない場合の平均スコアはnull（境界値）")
    void test_getAverageScore_noEvaluated() throws Exception {
        Double avg = submissionService.getAverageScore(student.getId(), course.getId());

        assertThat(avg).isNull();
    }
}
