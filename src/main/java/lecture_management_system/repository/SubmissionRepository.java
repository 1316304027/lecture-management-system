package lecture_management_system.repository;

import lecture_management_system.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * 課題提出Repository
 * submissionsテーブルのDB操作を担当する。
 */
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    /**
     * 学生・課題の提出情報取得（重複提出チェックに使用）
     */
    Optional<Submission> findByStudent_IdAndAssignment_Id(
            Long studentId, Long assignmentId);

    /**
     * 学生の全提出履歴取得（提出済み課題の確認に使用）
     */
    List<Submission> findByStudent_Id(Long studentId);

    /**
     * 課題別提出一覧取得（講師の提出物確認画面SCR-104で使用）
     * 提出日時の降順で返す。
     */
    List<Submission> findByAssignment_IdOrderBySubmittedAtDesc(Long assignmentId);

    /**
     * 学生のコース内提出数カウント（published問わず）
     */
    long countByStudent_IdAndAssignment_Course_Id(Long studentId, Long courseId);

    /**
     * 学生のコース内【公開課題のみ】提出数カウント（レポート集計用）
     * 未公開課題への提出は除外する。
     */
    long countByStudent_IdAndAssignment_Course_IdAndAssignment_PublishedTrue(
            Long studentId, Long courseId);

    /**
     * 学生・コースの提出一覧（提出履歴表示に使用）
     */
    List<Submission> findByStudent_IdAndAssignment_Course_Id(
            Long studentId, Long courseId);

    /**
     * 学生・コースの【公開課題のみ】提出一覧（平均スコア計算用）
     * 未公開課題への提出は除外する。
     */
    List<Submission> findByStudent_IdAndAssignment_Course_IdAndAssignment_PublishedTrue(
            Long studentId, Long courseId);
}
