package lecture_management_system.service;

import lecture_management_system.entity.*;
import lecture_management_system.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * MaterialService 単体テスト
 *
 * テスト観点：
 *  UT-MAT-01 正常系：教材を正常にアップロードできる
 *  UT-MAT-02 異常系：空ファイルはエラー
 *  UT-MAT-03 異常系：PDF以外のファイルはエラー
 *  UT-MAT-04 異常系：タイトル空文字はエラー
 *  UT-MAT-05 異常系：タイトル101文字はエラー（境界値）
 *  UT-MAT-06 正常系：タイトル100文字はOK（境界値）
 *  UT-MAT-07 正常系：公開状態を切り替えられる（非公開→公開）
 *  UT-MAT-08 正常系：受講者向けは公開教材のみ取得できる
 *  UT-MAT-09 正常系：教材を削除できる
 */
@SpringBootTest
@Transactional
@DisplayName("MaterialService 単体テスト")
class MaterialServiceTest {

    @Autowired private MaterialService materialService;
    @Autowired private CourseRepository courseRepository;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private UserRepository userRepository;

    private Course course;
    private User instructor;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setName("教材テストコース");
        courseRepository.save(course);

        instructor = new User();
        instructor.setName("テスト講師");
        instructor.setEmail("mat_instructor@example.com");
        instructor.setPassword("dummy");
        instructor.setRole("INSTRUCTOR");
        instructor.setActive(true);
        instructor.setLocked(false);
        instructor.setLoginFailureCount(0);
        userRepository.save(instructor);
    }

    // ----------------------------------------------------------------
    // UT-MAT-01: 正常なPDFアップロード
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-MAT-01: PDFを正常にアップロードできる")
    void test_uploadMaterial_success() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "lecture.pdf", "application/pdf", "content".getBytes());

        String error = materialService.uploadMaterial(
                course.getId(), "第1回講義資料", true, pdf, instructor);

        assertThat(error).isNull();
        List<Material> list = materialRepository.findByCourse_Id(course.getId());
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getTitle()).isEqualTo("第1回講義資料");
        assertThat(list.get(0).getPublished()).isTrue();
    }

    // ----------------------------------------------------------------
    // UT-MAT-02: 空ファイルはエラー
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-MAT-02: 空ファイルをアップロードするとエラーメッセージが返る")
    void test_uploadMaterial_emptyFile() throws Exception {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        String error = materialService.uploadMaterial(
                course.getId(), "空ファイル教材", false, empty, instructor);

        assertThat(error).isNotNull();
        assertThat(error).contains("ファイル");
    }

    // ----------------------------------------------------------------
    // UT-MAT-03: PDF以外はエラー
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-MAT-03: PDF以外のファイルをアップロードするとエラーメッセージが返る")
    void test_uploadMaterial_notPdf() throws Exception {
        MockMultipartFile txt = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "not pdf".getBytes());

        String error = materialService.uploadMaterial(
                course.getId(), "テキスト教材", false, txt, instructor);

        assertThat(error).isNotNull();
        assertThat(error).contains("PDF");
    }

    // ----------------------------------------------------------------
    // UT-MAT-04: タイトル空文字はエラー
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-MAT-04: タイトルが空文字の場合はエラーメッセージが返る")
    void test_uploadMaterial_emptyTitle() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "f.pdf", "application/pdf", "x".getBytes());

        String error = materialService.uploadMaterial(
                course.getId(), "  ", false, pdf, instructor);

        assertThat(error).isNotNull();
        assertThat(error).contains("タイトル");
    }

    // ----------------------------------------------------------------
    // UT-MAT-05: タイトル101文字はエラー（境界値）
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-MAT-05: タイトルが101文字の場合はエラーメッセージが返る（境界値）")
    void test_uploadMaterial_titleTooLong() throws Exception {
        String title101 = "あ".repeat(101);
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "f.pdf", "application/pdf", "x".getBytes());

        String error = materialService.uploadMaterial(
                course.getId(), title101, false, pdf, instructor);

        assertThat(error).isNotNull();
        assertThat(error).contains("100文字");
    }

    // ----------------------------------------------------------------
    // UT-MAT-06: タイトル100文字はOK（境界値）
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-MAT-06: タイトルがちょうど100文字の場合は正常に登録できる（境界値）")
    void test_uploadMaterial_title100Chars() throws Exception {
        String title100 = "あ".repeat(100);
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "f.pdf", "application/pdf", "x".getBytes());

        String error = materialService.uploadMaterial(
                course.getId(), title100, false, pdf, instructor);

        assertThat(error).isNull();
    }

    // ----------------------------------------------------------------
    // UT-MAT-07: 公開状態切替（非公開 → 公開）
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-MAT-07: 非公開教材を公開状態に切り替えられる")
    void test_togglePublished() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "f.pdf", "application/pdf", "x".getBytes());
        materialService.uploadMaterial(
                course.getId(), "切替教材", false, pdf, instructor);

        Material m = materialRepository.findByCourse_Id(course.getId()).get(0);
        assertThat(m.getPublished()).isFalse();

        materialService.togglePublished(m.getId());

        assertThat(materialRepository.findById(m.getId()).get().getPublished()).isTrue();
    }

    // ----------------------------------------------------------------
    // UT-MAT-08: 受講者向けは公開教材のみ取得
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-MAT-08: findPublishedByCourseId は公開教材のみ返す")
    void test_findPublishedByCourseId() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "f.pdf", "application/pdf", "x".getBytes());

        materialService.uploadMaterial(course.getId(), "公開教材1", true,  pdf, instructor);
        materialService.uploadMaterial(course.getId(), "公開教材2", true,  pdf, instructor);
        materialService.uploadMaterial(course.getId(), "非公開教材", false, pdf, instructor);

        List<Material> published = materialService.findPublishedByCourseId(course.getId());

        assertThat(published).hasSize(2);
        assertThat(published).allMatch(Material::getPublished);
    }

    // ----------------------------------------------------------------
    // UT-MAT-09: 教材削除
    // ----------------------------------------------------------------
    @Test
    @DisplayName("UT-MAT-09: 教材を削除できる")
    void test_deleteMaterial() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "f.pdf", "application/pdf", "x".getBytes());
        materialService.uploadMaterial(
                course.getId(), "削除対象教材", false, pdf, instructor);

        Material m = materialRepository.findByCourse_Id(course.getId()).get(0);
        materialService.deleteMaterial(m.getId());

        assertThat(materialRepository.findById(m.getId())).isEmpty();
    }
}