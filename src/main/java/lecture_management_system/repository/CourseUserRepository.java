package lecture_management_system.repository;

import lecture_management_system.entity.CourseUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * コース所属Repository
 * course_usersテーブルのDB操作を担当する。
 *
 * このRepositoryが講師・学生とコースの紐付けの中心。
 * 管理者がコース割当を行う際、および
 * 講師・学生が自分のコース一覧を取得する際に使用する。
 */
public interface CourseUserRepository extends JpaRepository<CourseUser, Long> {

    /**
     * コースID + ロールで所属ユーザー一覧取得
     * 例：課程の担当講師一覧 → role="INSTRUCTOR"
     *     課程の受講学生一覧 → role="STUDENT"
     */
    List<CourseUser> findByCourse_IdAndRole(Long courseId, String role);

    /**
     * ユーザーID + ロールで所属コース一覧取得
     * 例：講師の担当コース → role="INSTRUCTOR"
     *     学生の受講コース → role="STUDENT"
     */
    List<CourseUser> findByUser_IdAndRole(Long userId, String role);

    /** ユーザーIDで全所属コース取得（ユーザー削除時の一括解除用） */
    List<CourseUser> findByUser_Id(Long userId);

    /** コースIDで全所属ユーザー取得 */
    List<CourseUser> findByCourse_Id(Long courseId);

    /** 特定のコース・ユーザーの所属情報取得（重複チェックに使用） */
    Optional<CourseUser> findByCourse_IdAndUser_Id(Long courseId, Long userId);

    /** コース・ユーザーの所属存在確認 */
    boolean existsByCourse_IdAndUser_Id(Long courseId, Long userId);
}
