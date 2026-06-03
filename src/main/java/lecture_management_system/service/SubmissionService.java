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
 * 課題提出業務Service / Assignment Submission Business Service
 *
 * 課題提出・提出一覧取得・講師評価（コメント＋スコア）を担当する。
 * Handles assignment submission, retrieval, and instructor evaluation (comment + score).
 *
 * ================================================================
 * 【変更点③】saveEvaluation メソッドにスコア引数を追加
 * [Change ③] Added score parameter to saveEvaluation method
 *
 * 講師がスコア（0〜100）とコメントを同時に保存できるようにした。
 * Instructors can now save both score (0-100) and comment together.
 * ================================================================
 */
@Service
public class SubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    /** ファイル保存ディレクトリ（application.yamlで設定） / Upload directory */
    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    /**
     * 課題提出（学生SCR-005から使用）
     * Assignment Submission (used from student SCR-005)
     *
     * 【バリデーション順序】/ [Validation order]
     * 1. 期限切れチェック / Deadline check
     * 2. 重複提出チェック / Duplicate submission check
     * 3. ファイル空チェック / Empty file check
     * 4. PDF形式チェック / PDF format check
     * 5. ファイルサイズチェック（10MB以内）/ File size check (max 10MB)
     *
     * @return "success" / エラーコード文字列 / error code string
     */
    public String submit(User student, Assignment assignment,
                         MultipartFile file) throws IOException {
        // 1. 期限切れチェック / Deadline check
        if (LocalDateTime.now().isAfter(assignment.getDeadline())) {
            return "deadline";
        }
        // 2. 重複チェック / Duplicate check
        if (submissionRepository.findByStudent_IdAndAssignment_Id(
                student.getId(), assignment.getId()).isPresent()) {
            return "already";
        }
        // 3. ファイル空チェック / Empty file check
        if (file == null || file.isEmpty()) return "empty";

        String originalName = file.getOriginalFilename();
        // 4. PDF形式チェック / PDF format check
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            return "pdf_only";
        }
        // 5. サイズチェック（10MB）/ Size check (10MB)
        if (file.getSize() > 10L * 1024 * 1024) return "too_large";

        // ファイル保存 / Save file to disk
        Files.createDirectories(Paths.get(uploadDir));
        String storedFileName = System.currentTimeMillis() + "_" + originalName;
        Files.copy(file.getInputStream(),
                Paths.get(uploadDir, storedFileName),
                StandardCopyOption.REPLACE_EXISTING);

        // DB登録 / Save to DB
        Submission submission = new Submission();
        submission.setStudent(student);
        submission.setAssignment(assignment);
        submission.setStoredFileName(storedFileName);
        submission.setOriginalFileName(originalName);
        submission.setSubmittedAt(LocalDateTime.now());
        submissionRepository.save(submission);
        return "success";
    }

    /**
     * 課題別提出一覧取得（講師SCR-104から使用）
     * Get submissions by assignment (used from instructor SCR-104)
     * 提出日時降順で返す。 / Returns in descending order of submission time.
     */
    public List<Submission> getByAssignment(Long assignmentId) {
        return submissionRepository.findByAssignment_IdOrderBySubmittedAtDesc(assignmentId);
    }

    /**
     * 学生の提出一覧取得（学生画面での提出状況確認に使用）
     * Get submissions by student (used for student status display)
     */
    public List<Submission> getByStudent(Long studentId) {
        return submissionRepository.findByStudent_Id(studentId);
    }

    /** 提出情報詳細取得 / Get submission detail by ID */
    public Submission findById(Long id) {
        return submissionRepository.findById(id).orElse(null);
    }

    /**
     * 【変更点③】講師による評価保存（コメント＋スコア）
     * [Change ③] Save instructor evaluation (comment + score)
     *
     * コメントとスコアを同時に保存し、評価日時を記録する。
     * Saves both comment and score together, and records evaluation timestamp.
     *
     * @param submissionId  対象提出ID / Target submission ID
     * @param comment       評価コメント（null可）/ Evaluation comment (nullable)
     * @param score         評価スコア 0〜100（null可）/ Score 0-100 (nullable)
     */
    public void saveEvaluation(Long submissionId, String comment, Integer score) {
        Submission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission != null) {
            // コメントを設定 / Set comment
            submission.setEvaluationComment(comment);
            // スコアを設定（0〜100の範囲チェック）/ Set score (validate 0-100 range)
            if (score != null) {
                // 0未満や100超は強制的にクランプ / Clamp to valid range
                score = Math.max(0, Math.min(100, score));
            }
            submission.setScore(score);
            // 評価日時を現在時刻に設定 / Set evaluation timestamp to now
            submission.setEvaluatedAt(LocalDateTime.now());
            submissionRepository.save(submission);
        }
    }

    /** ファイルパス取得 / Get file path for download */
    public Path getFilePath(String storedFileName) {
        return Paths.get(uploadDir, storedFileName);
    }

    /**
     * 学生のコース内提出数カウント（レポート集計で使用）
     * Count submissions by student and course (used in report aggregation)
     */
    public long countByStudentAndCourse(Long studentId, Long courseId) {
        return submissionRepository.countByStudent_IdAndAssignment_Course_Id(studentId, courseId);
    }

    /**
     * 【変更点③】学生のコース内評価済み提出の平均スコアを取得
     * [Change ③] Get average score for a student's submissions in a course
     *
     * スコアが設定されている（評価済み）提出のみを対象とする。
     * Only considers submissions that have a score set (evaluated).
     *
     * @param studentId 学生ID / Student ID
     * @param courseId  コースID / Course ID
     * @return 平均スコア。評価済み提出がない場合は null / Average score, null if no evaluated submissions
     */
    public Double getAverageScore(Long studentId, Long courseId) {
        List<Submission> submissions = submissionRepository
                .findByStudent_IdAndAssignment_Course_Id(studentId, courseId);
        // スコアが設定されている提出のみ抽出 / Filter to only evaluated submissions
        List<Integer> scores = submissions.stream()
                .filter(s -> s.getScore() != null)
                .map(Submission::getScore)
                .toList();
        if (scores.isEmpty()) return null;
        // 平均を計算して小数点1桁に丸める / Calculate average, round to 1 decimal
        double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
        return Math.round(avg * 10) / 10.0;
    }
}
