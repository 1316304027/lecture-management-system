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
 * 課題提出業務Service
 * 【S3対応】提出PDFファイルの保存先をS3に変更。
 */
@Service
public class SubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private S3Service s3Service;

    private static final String S3_PREFIX = "submissions/";

    /**
     * 課題提出（学生SCR-005から使用）
     * 【S3対応】提出PDFをS3にアップロードする。
     *
     * @return "success" / エラーコード文字列
     */
    public String submit(User student, Assignment assignment,
                         MultipartFile file) throws IOException {
        // 1. 期限切れチェック
        if (LocalDateTime.now().isAfter(assignment.getDeadline())) return "deadline";
        // 2. 重複チェック
        if (submissionRepository.findByStudent_IdAndAssignment_Id(
                student.getId(), assignment.getId()).isPresent()) return "already";
        // 3. ファイル空チェック
        if (file == null || file.isEmpty()) return "empty";

        String originalName = file.getOriginalFilename();
        // 4. PDF形式チェック
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) return "pdf_only";
        // 5. サイズチェック（10MB）
        if (file.getSize() > 10L * 1024 * 1024) return "too_large";

        // S3にアップロード
        String storedFileName = System.currentTimeMillis() + "_" + originalName;
        String s3Key = S3_PREFIX + storedFileName;
        s3Service.uploadFile(file, s3Key);

        // DB登録（S3キーを保存）
        Submission submission = new Submission();
        submission.setStudent(student);
        submission.setAssignment(assignment);
        submission.setStoredFileName(s3Key);
        submission.setOriginalFileName(originalName);
        submission.setSubmittedAt(LocalDateTime.now());
        submissionRepository.save(submission);
        return "success";
    }

    public List<Submission> getByAssignment(Long assignmentId) {
        return submissionRepository.findByAssignment_IdOrderBySubmittedAtDesc(assignmentId);
    }

    public List<Submission> getByStudent(Long studentId) {
        return submissionRepository.findByStudent_Id(studentId);
    }

    public Submission findById(Long id) {
        return submissionRepository.findById(id).orElse(null);
    }

    public void saveEvaluation(Long submissionId, String comment, Integer score) {
        Submission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission != null) {
            submission.setEvaluationComment(comment);
            if (score != null) {
                score = Math.max(0, Math.min(100, score));
            }
            submission.setScore(score);
            submission.setEvaluatedAt(LocalDateTime.now());
            submissionRepository.save(submission);
        }
    }

    /**
     * ダウンロード用S3署名付きURLを取得する
     * 【S3対応】ローカルパスの代わりに署名付きURLを返す。
     */
    public String getDownloadUrl(String storedFileName) {
        return s3Service.generatePresignedUrl(storedFileName);
    }

    public long countByStudentAndCourse(Long studentId, Long courseId) {
        return submissionRepository.countByStudent_IdAndAssignment_Course_Id(studentId, courseId);
    }

    public Double getAverageScore(Long studentId, Long courseId) {
        List<Submission> submissions = submissionRepository
                .findByStudent_IdAndAssignment_Course_Id(studentId, courseId);
        List<Integer> scores = submissions.stream()
                .filter(s -> s.getScore() != null)
                .map(Submission::getScore)
                .toList();
        if (scores.isEmpty()) return null;
        double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
        return Math.round(avg * 10) / 10.0;
    }
}
