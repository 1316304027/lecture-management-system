package lecture_management_system.repository;

import lecture_management_system.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 課題Repository
 * assignmentsテーブルのDB操作を担当する。
 */
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    /**
     * コース別課題一覧取得（全件：講師管理用）
     * 公開・非公開問わず全課題を返す。
     */
    List<Assignment> findByCourse_Id(Long courseId);

    /**
     * コース別公開課題一覧取得（受講者用）
     * published=true の課題のみ返す。
     */
    List<Assignment> findByCourse_IdAndPublishedTrue(Long courseId);

    /** コースの課題数カウント（全件） */
    long countByCourse_Id(Long courseId);

    /** コースの【公開課題のみ】カウント（レポート集計用） */
    long countByCourse_IdAndPublishedTrue(Long courseId);
}
