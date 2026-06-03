package lecture_management_system.service;

import lecture_management_system.entity.*;
import lecture_management_system.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * AssignmentService 単体テスト
 *
 * テスト観点：
 *  UT-ASN-01 正常系：課題を作成できる（ファイルなし）
 *  UT-ASN-02 異常系：タイトル空文字はエラー
 *  UT-ASN-03 異常系：締切が過去日時はエラー
 *  UT-ASN-04 正常系：公開状態を切り替えられる（非公開→公開）
 *  UT-ASN-05 正常系：公開状態を切り替えられる（公開→非公開）
 *  UT-ASN-06 正常系：コース別公開課題のみ取得できる
 *  UT-ASN-07 正常系：1件削除できる
 *  UT-ASN-08 正常系：コース全課題を一括削除できる
 *  UT-ASN-09 境界値：締切ちょうどの日時は作成できる
 */
@SpringBootTest
@Transactional
@DisplayName("AssignmentService 単体テスト")
class AssignmentServiceTest {

    @Autowired private AssignmentService assignmentService;
    @Autowired private CourseRepository courseRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private UserRepository userRepository;

    private Course course;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setName("課題テストコース");
        courseRepository.save(course);
    }

    // ----------------------------------------------------------------
    // UT-ASN-01: 課題作成（ファイルなし）
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-ASN-01: 課題をファイルなしで正常に作成できる")
    void test_createAssignment_success() throws Exception {
        LocalDateTime future = LocalDateTime.now().plusDays(7);

        String error = assignmentService.createAssignment(
                course.getId(), "Javaレポート課題", future, true, null);

        assertThat(error).isNull();
        List<Assignment> list = assignmentRepository.findByCourse_Id(course.getId());
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getTitle()).isEqualTo("Javaレポート課題");
        assertThat(list.get(0).getPublished()).isTrue();
    }

    // ----------------------------------------------------------------
    // UT-ASN-02: タイトル空文字はエラー
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-ASN-02: タイトルが空文字の場合はエラーメッセージが返る")
    void test_createAssignment_emptyTitle() throws Exception {
        String error = assignmentService.createAssignment(
                course.getId(), "  ", LocalDateTime.now().plusDays(1), false, null);

        assertThat(error).isNotNull();
        assertThat(error).contains("タイトル");
    }

    // ----------------------------------------------------------------
    // UT-ASN-03: 締切が過去日時はエラー
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-ASN-03: 締切日時が過去の場合はエラーメッセージが返る")
    void test_createAssignment_pastDeadline() throws Exception {
        LocalDateTime past = LocalDateTime.now().minusHours(1);

        String error = assignmentService.createAssignment(
                course.getId(), "過去締切課題", past, false, null);

        assertThat(error).isNotNull();
        assertThat(error).contains("現在日時");
    }

    // ----------------------------------------------------------------
    // UT-ASN-04: 公開状態切替（非公開 → 公開）
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-ASN-04: 非公開課題を公開状態に切り替えられる")
    void test_togglePublished_falseToTrue() throws Exception {
        assignmentService.createAssignment(
                course.getId(), "切替テスト課題", LocalDateTime.now().plusDays(1), false, null);
        Assignment a = assignmentRepository.findByCourse_Id(course.getId()).get(0);
        assertThat(a.getPublished()).isFalse();

        assignmentService.togglePublished(a.getId());

        assertThat(assignmentRepository.findById(a.getId()).get().getPublished()).isTrue();
    }

    // ----------------------------------------------------------------
    // UT-ASN-05: 公開状態切替（公開 → 非公開）
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-ASN-05: 公開中課題を非公開状態に切り替えられる")
    void test_togglePublished_trueToFalse() throws Exception {
        assignmentService.createAssignment(
                course.getId(), "公開課題", LocalDateTime.now().plusDays(1), true, null);
        Assignment a = assignmentRepository.findByCourse_Id(course.getId()).get(0);
        assertThat(a.getPublished()).isTrue();

        assignmentService.togglePublished(a.getId());

        assertThat(assignmentRepository.findById(a.getId()).get().getPublished()).isFalse();
    }

    // ----------------------------------------------------------------
    // UT-ASN-06: 受講者向け公開課題のみ取得できる
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-ASN-06: findPublishedByCourseId は公開課題のみ返す")
    void test_findPublishedByCourseId() throws Exception {
        assignmentService.createAssignment(
                course.getId(), "公開A", LocalDateTime.now().plusDays(1), true, null);
        assignmentService.createAssignment(
                course.getId(), "公開B", LocalDateTime.now().plusDays(2), true, null);
        assignmentService.createAssignment(
                course.getId(), "非公開C", LocalDateTime.now().plusDays(3), false, null);

        List<Assignment> published = assignmentService.findPublishedByCourseId(course.getId());

        assertThat(published).hasSize(2);
        assertThat(published).allMatch(Assignment::getPublished);
    }

    // ----------------------------------------------------------------
    // UT-ASN-07: 1件削除
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-ASN-07: 課題を1件削除できる")
    void test_deleteAssignment() throws Exception {
        assignmentService.createAssignment(
                course.getId(), "削除対象課題", LocalDateTime.now().plusDays(1), true, null);
        Assignment a = assignmentRepository.findByCourse_Id(course.getId()).get(0);

        assignmentService.deleteAssignment(a.getId());

        assertThat(assignmentRepository.findById(a.getId())).isEmpty();
    }

    // ----------------------------------------------------------------
    // UT-ASN-08: コース全課題一括削除
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-ASN-08: deleteAllByCourseId でコースの全課題が削除される")
    void test_deleteAllByCourseId() throws Exception {
        for (int i = 1; i <= 3; i++) {
            assignmentService.createAssignment(
                    course.getId(), "課題" + i, LocalDateTime.now().plusDays(i), true, null);
        }
        assertThat(assignmentRepository.findByCourse_Id(course.getId())).hasSize(3);

        assignmentService.deleteAllByCourseId(course.getId());

        assertThat(assignmentRepository.findByCourse_Id(course.getId())).isEmpty();
    }

    // ----------------------------------------------------------------
    // UT-ASN-09: 締切ちょうど1秒後は作成できる（境界値）
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-ASN-09: 締切が現在より1秒後（境界値）は作成できる")
    void test_createAssignment_deadlineJustFuture() throws Exception {
        LocalDateTime justFuture = LocalDateTime.now().plusSeconds(5);

        String error = assignmentService.createAssignment(
                course.getId(), "境界値課題", justFuture, false, null);

        assertThat(error).isNull();
    }
}
