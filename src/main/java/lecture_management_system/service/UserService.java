package lecture_management_system.service;

import jakarta.annotation.PostConstruct;
import lecture_management_system.entity.User;
import lecture_management_system.repository.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

/**
 * ユーザー業務Service
 * ユーザーCRUD、パスワード認証、アカウントロック管理を担当する。
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseUserRepository courseUserRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    /** メールアドレスでユーザーを検索（ログイン認証で使用） */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /** IDでユーザーを取得 */
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    /** 全ユーザー一覧取得（管理者画面SCR-202で使用） */
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /** ロール別ユーザー一覧取得（コース割当時に講師・学生一覧を取得） */
    public List<User> findByRole(String role) {
        return userRepository.findByRole(role);
    }

    /** IDリストでユーザー一覧取得 */
    public List<User> findByIds(List<Long> ids) {
        return userRepository.findAllById(ids);
    }

    /** ユーザー保存（セッション更新・ロック解除などで使用） */
    public User save(User user) {
        return userRepository.save(user);
    }

    /**
     * パスワード認証
     * BCryptでハッシュ化されたパスワードと入力パスワードを比較する。
     * @return 一致すればtrue
     */
    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * ユーザー新規作成（管理者SCR-202から使用）
     * パスワードをBCryptハッシュ化してDBに保存する。
     * @return エラーメッセージ（null = 成功）
     */
    public String createUser(String name, String email, String password, String role) {
        // メールアドレス重複チェック
        if (userRepository.existsByEmail(email)) {
            return "既に登録済みのメールアドレスです";
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password)); // BCryptハッシュ化
        user.setRole(role);
        user.setActive(true);
        user.setLocked(false);
        user.setLoginFailureCount(0);
        userRepository.save(user);
        return null;
    }

    /**
     * ユーザー更新（管理者SCR-202から使用）
     * パスワードが空の場合は変更しない。
     * @return エラーメッセージ（null = 成功）
     */
    public String updateUser(Long id, String name, String email, String password, String role) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return "ユーザーが見つかりません";
        // メールアドレス重複チェック（自分以外）
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            return "既に登録済みのメールアドレスです";
        }
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        if (password != null && !password.isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        userRepository.save(user);
        return null;
    }

    /**
     * ユーザー削除（管理者SCR-202から使用）
     *
     * 【修正】外部キー制約によるページ崩壊を防ぐため
     * 関連データを先に全削除してからユーザーを削除する。
     * 削除順序: submissions → attendances → course_users → user
     */
    @Transactional
    public void deleteUser(Long id) {
        // 1. 提出物を削除
        submissionRepository.deleteAll(submissionRepository.findByStudent_Id(id));
        // 2. 出席データを全削除
        attendanceRepository.deleteAll(attendanceRepository.findByStudent_Id(id));
        // 3. コース所属を解除（findByUser_IdAndRole ではなく全ロール一括）
        courseUserRepository.deleteAll(courseUserRepository.findByUser_Id(id));
        // 4. ユーザー本体削除
        userRepository.deleteById(id);
    }

    /**
     * アカウントロック解除（管理者が手動解除）
     * lockedをfalseに、login_failure_countを0にリセットする。
     */
    public void unlockUser(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setLocked(false);
            user.setLoginFailureCount(0);
            userRepository.save(user);
        }
    }

    /**
     * アプリ起動時にデフォルトユーザーを作成する（開発用）
     * 本番環境では削除またはコメントアウトすること。
     */
    @PostConstruct
    public void createDefaultUsers() {
        // 起動するたびに必ずパスワードをBCryptで上書き保存する
        // 旧コードの平文パスワードが残っていても、これで上書きされてログインできる
        createOrUpdateUser("管理者",    "admin@test.com",    "Admin1234", "ADMIN");
        createOrUpdateUser("講師-奈良鹿丸",  "teacher@test.com",  "Admin1234", "INSTRUCTOR");
        createOrUpdateUser("受講者-渡辺勇大", "student@test.com",  "Admin1234", "STUDENT");
    }

    private void createOrUpdateUser(String name, String email,
                                    String password, String role) {
        // すでに同じメールのユーザーがいれば取得、なければ新規作成
        User user = userRepository.findByEmail(email).orElse(new User());
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password)); // BCryptで暗号化して上書き
        user.setRole(role);
        user.setActive(true);
        user.setLocked(false);
        user.setLoginFailureCount(0);
        userRepository.save(user);
    }
}

