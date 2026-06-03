package lecture_management_system.service;

import lecture_management_system.entity.*;
import lecture_management_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 課題業務Service
 * 課題の作成・一覧取得・公開管理を担当する。
 */
@Service
public class AssignmentService {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private lecture_management_system.repository.SubmissionRepository submissionRepository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    /** コース別課題一覧取得（講師用：全件） */
    public List<Assignment> findByCourseId(Long courseId) {
        return assignmentRepository.findByCourse_Id(courseId);
    }

    /** コース別公開課題一覧取得（受講者用） */
    public List<Assignment> findPublishedByCourseId(Long courseId) {
        return assignmentRepository.findByCourse_IdAndPublishedTrue(courseId);
    }

    /** 課題詳細取得 */
    public Assignment findById(Long id) {
        return assignmentRepository.findById(id).orElse(null);
    }

    /**
     * 課題作成（講師SCR-103から使用）
     *
     * 【バリデーション】
     * - タイトル必須・100文字以内
     * - 締切日時必須・現在日時以降
     * - 添付PDFはオプション（ある場合はPDF・10MB以内）
     *
     * @return エラーメッセージ（null = 成功）
     */
    public String createAssignment(Long courseId, String title,
                                   LocalDateTime deadline, boolean published,
                                   MultipartFile file) throws IOException {
        // バリデーション
        if (title == null || title.trim().isEmpty()) return "課題タイトルを入力してください";
        if (deadline == null) return "締切日時を入力してください";
        if (deadline.isBefore(LocalDateTime.now())) return "現在日時以降を入力してください";

        String storedFileName = null;
        if (file != null && !file.isEmpty()) {
            String originalName = file.getOriginalFilename();
            if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
                return "PDF形式のみアップロード可能です";
            }
            if (file.getSize() > 10L * 1024 * 1024) {
                return "ファイルサイズ上限（10MB）を超えています";
            }
            Files.createDirectories(Paths.get(uploadDir));
            storedFileName = System.currentTimeMillis() + "_" + originalName;
            Files.copy(file.getInputStream(),
                    Paths.get(uploadDir, storedFileName),
                    StandardCopyOption.REPLACE_EXISTING);
        }

        Course course = courseRepository.findById(courseId).orElse(null);
        Assignment assignment = new Assignment();
        assignment.setCourse(course);
        assignment.setTitle(title.trim());
        assignment.setDeadline(deadline);
        assignment.setPublished(published);
        assignment.setStoredFileName(storedFileName != null ? storedFileName : "");
        assignmentRepository.save(assignment);
        return null;
    }

    /** 公開状態の切り替え */
    public void togglePublished(Long assignmentId) {
        Assignment a = assignmentRepository.findById(assignmentId).orElse(null);
        if (a != null) {
            a.setPublished(!a.getPublished());
            assignmentRepository.save(a);
        }
    }

    /** 課題削除 */
    public void deleteAssignment(Long id) {
        assignmentRepository.deleteById(id);
    }

    /**
     * コースの全課題削除（講師管理用）
     * 【新規追加】提出物も先に削除してから課題を削除する。
     */
    @org.springframework.transaction.annotation.Transactional
    public void deleteAllByCourseId(Long courseId) {
        List<Assignment> assignments = assignmentRepository.findByCourse_Id(courseId);
        for (Assignment a : assignments) {
            // 提出物を先に削除
            submissionRepository.deleteAll(
                submissionRepository.findByAssignment_IdOrderBySubmittedAtDesc(a.getId())
            );
        }
        assignmentRepository.deleteAll(assignments);
    }

    /** ファイルパス取得 */
    public Path getFilePath(String storedFileName) {
        return Paths.get(uploadDir, storedFileName);
    }
}
