package lecture_management_system.service;

import lecture_management_system.entity.*;
import lecture_management_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 課題業務Service
 * 【S3対応】課題PDFファイルの保存先をS3に変更。
 */
@Service
public class AssignmentService {

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private lecture_management_system.repository.SubmissionRepository submissionRepository;

    @Autowired
    private S3Service s3Service;

    private static final String S3_PREFIX = "assignments/";

    public List<Assignment> findByCourseId(Long courseId) {
        return assignmentRepository.findByCourse_Id(courseId);
    }

    public List<Assignment> findPublishedByCourseId(Long courseId) {
        return assignmentRepository.findByCourse_IdAndPublishedTrue(courseId);
    }

    public Assignment findById(Long id) {
        return assignmentRepository.findById(id).orElse(null);
    }

    /**
     * 課題作成（講師SCR-103から使用）
     * 【S3対応】添付PDFをS3にアップロードする。
     *
     * @return エラーメッセージ（null = 成功）
     */
    public String createAssignment(Long courseId, String title,
                                   LocalDateTime deadline, boolean published,
                                   MultipartFile file) throws IOException {
        if (title == null || title.trim().isEmpty()) return "課題タイトルを入力してください";
        if (deadline == null) return "締切日時を入力してください";
        if (deadline.isBefore(LocalDateTime.now())) return "現在日時以降を入力してください";

        String s3Key = null;
        if (file != null && !file.isEmpty()) {
            String originalName = file.getOriginalFilename();
            if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
                return "PDF形式のみアップロード可能です";
            }
            if (file.getSize() > 10L * 1024 * 1024) {
                return "ファイルサイズ上限（10MB）を超えています";
            }
            // S3にアップロード
            String storedFileName = System.currentTimeMillis() + "_" + originalName;
            s3Key = S3_PREFIX + storedFileName;
            s3Service.uploadFile(file, s3Key);
        }

        Course course = courseRepository.findById(courseId).orElse(null);
        Assignment assignment = new Assignment();
        assignment.setCourse(course);
        assignment.setTitle(title.trim());
        assignment.setDeadline(deadline);
        assignment.setPublished(published);
        assignment.setStoredFileName(s3Key != null ? s3Key : "");
        assignmentRepository.save(assignment);
        return null;
    }

    public void togglePublished(Long assignmentId) {
        Assignment a = assignmentRepository.findById(assignmentId).orElse(null);
        if (a != null) {
            a.setPublished(!a.getPublished());
            assignmentRepository.save(a);
        }
    }

    /**
     * 課題削除
     * 【S3対応】S3からファイルも削除する。
     */
    public void deleteAssignment(Long id) {
        Assignment a = assignmentRepository.findById(id).orElse(null);
        if (a != null) {
            if (a.getStoredFileName() != null && !a.getStoredFileName().isBlank()) {
                s3Service.deleteFile(a.getStoredFileName());
            }
            assignmentRepository.deleteById(id);
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteAllByCourseId(Long courseId) {
        List<Assignment> assignments = assignmentRepository.findByCourse_Id(courseId);
        for (Assignment a : assignments) {
            if (a.getStoredFileName() != null && !a.getStoredFileName().isBlank()) {
                s3Service.deleteFile(a.getStoredFileName());
            }
            submissionRepository.deleteAll(
                submissionRepository.findByAssignment_IdOrderBySubmittedAtDesc(a.getId())
            );
        }
        assignmentRepository.deleteAll(assignments);
    }

    /**
     * ダウンロード用S3署名付きURLを取得する
     */
    public String getDownloadUrl(String storedFileName) {
        return s3Service.generatePresignedUrl(storedFileName);
    }
}
