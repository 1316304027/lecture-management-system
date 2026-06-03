package lecture_management_system.repository;

import lecture_management_system.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 教材Repository
 * materialsテーブルのDB操作を担当する。
 */
public interface MaterialRepository extends JpaRepository<Material, Long> {

    /**
     * コース別教材一覧取得（全件：講師管理用）
     * 公開・非公開問わず全教材を返す。
     */
    List<Material> findByCourse_Id(Long courseId);

    /**
     * コース別公開教材一覧取得（受講者用）
     * published=true の教材のみ返す。
     */
    List<Material> findByCourse_IdAndPublishedTrue(Long courseId);
}
