package lecture_management_system.repository;

import lecture_management_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * ユーザーRepository
 * usersテーブルのDB操作を担当する。
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** メールアドレスでユーザーを検索（ログイン認証で使用） */
    Optional<User> findByEmail(String email);

    /** ロール別ユーザー一覧取得（コース割当画面で講師・学生一覧を取得する） */
    List<User> findByRole(String role);

    /** メールアドレスの重複確認（ユーザー登録時のバリデーション） */
    boolean existsByEmail(String email);

    /** メールアドレスの重複確認（更新時：自分のID以外で重複しないか確認） */
    boolean existsByEmailAndIdNot(String email, Long id);
}
