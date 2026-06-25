package lecture_management_system.service;

import jakarta.annotation.PostConstruct;
import lecture_management_system.dto.PasswordOperationResult;
import lecture_management_system.entity.User;
import lecture_management_system.entity.Course;
import lecture_management_system.entity.CourseUser;
import lecture_management_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired private UserRepository userRepository;
    @Autowired private CourseUserRepository courseUserRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;
    @Autowired private PasswordResetService passwordResetService;

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public List<User> findByRole(String role) {
        return userRepository.findByRole(role);
    }

    public List<User> findByIds(List<Long> ids) {
        return userRepository.findAllById(ids);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * ユーザー新規作成：管理者はパスワードを設定しない。
     * AWS SES でパスワード設定リンクをメール送信する。
     */
    @Transactional
    public PasswordOperationResult createUser(String name, String email, String role) {
        if (userRepository.existsByEmail(email)) {
            return PasswordOperationResult.fail("既に登録済みのメールアドレスです");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("UNSET_" + UUID.randomUUID()));
        user.setRole(role);
        user.setActive(true);
        user.setLocked(false);
        user.setLoginFailureCount(0);
        user.setPasswordResetRequired(true);
        user.setPasswordNotSet(true);
        userRepository.save(user);
        return passwordResetService.issueSetupEmail(user);
    }

    public String updateUser(Long id, String name, String email, String role) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return "ユーザーが見つかりません";
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            return "既に登録済みのメールアドレスです";
        }
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        userRepository.save(user);
        return null;
    }

    /**
     * 管理者リセット：パスワードを無効化し、SES で設定リンクを再送（管理者はパスワードを知らない）
     */
    @Transactional
    public PasswordOperationResult resetPasswordByAdmin(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return PasswordOperationResult.fail("ユーザーが見つかりません");
        if ("ADMIN".equals(user.getRole())) {
            return PasswordOperationResult.fail("管理者のパスワードはこの画面からリセットできません");
        }
        user.setPassword(passwordEncoder.encode("RESET_" + UUID.randomUUID()));
        user.setPasswordNotSet(true);
        user.setPasswordResetRequired(true);
        user.setLocked(false);
        user.setLoginFailureCount(0);
        userRepository.save(user);
        return passwordResetService.issueSetupEmail(user);
    }

    /**
     * メールリンクから本人がパスワードを設定
     */
    @Transactional
    public String setupPasswordViaToken(String token, String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            return "パスワードは8文字以上にしてください";
        }
        if (!newPassword.equals(confirmPassword)) {
            return "確認用パスワードが一致しません";
        }
        User user = passwordResetService.validateToken(token);
        if (user == null) {
            return "リンクが無効または期限切れです。管理者にパスワードリセットを依頼してください。";
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordNotSet(false);
        user.setPasswordResetRequired(false);
        userRepository.save(user);
        passwordResetService.consumeToken(token);
        return null;
    }

    /** ログイン後の自主変更（既にパスワード設定済みのユーザー用） */
    public String changeOwnPassword(Long userId, String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            return "パスワードは8文字以上にしてください";
        }
        if (!newPassword.equals(confirmPassword)) {
            return "確認用パスワードが一致しません";
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return "ユーザーが見つかりません";
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordNotSet(false);
        user.setPasswordResetRequired(false);
        userRepository.save(user);
        return null;
    }

    @Transactional
    public void deleteUser(Long id) {
        submissionRepository.deleteAll(submissionRepository.findByStudent_Id(id));
        attendanceRepository.deleteAll(attendanceRepository.findByStudent_Id(id));
        courseUserRepository.deleteAll(courseUserRepository.findByUser_Id(id));
        userRepository.deleteById(id);
    }

    public void unlockUser(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user != null) {
            user.setLocked(false);
            user.setLoginFailureCount(0);
            userRepository.save(user);
        }
    }

    @PostConstruct
    public void createDefaultUsers() {
        createOrUpdateUser("管理者",    "admin@test.com",    "Admin1234", "ADMIN");
        createOrUpdateUser("奈良鹿丸",  "teacher@test.com",  "Admin1234", "INSTRUCTOR");
        createOrUpdateUser("渡辺勇大", "student@test.com",  "Admin1234", "STUDENT");
    }

    /** 現在の受講者数（ロール=STUDENT） */
    public long countStudents() {
        return userRepository.findByRole("STUDENT").size();
    }

    /**
     * 【1回だけ手動実行用】不足分のデモ受講者を追加する。
     * 目標人数に達している場合は 0 を返し、何もしない。
     */
    @Transactional
    public int seedStudentsToTargetOnce(int targetTotal, Long assignToCourseId) {
        long current = countStudents();
        if (current >= targetTotal) return 0;
        int need = (int) (targetTotal - current);
        int created = seedAdditionalDemoStudents(need, assignToCourseId);
        log.info("[1回限定投入] 受講者 {} 名追加 → RDS users 表（合計 {} 名）", created, current + created);
        return created;
    }

    /**
     * デモ用受講者を need 名だけ作成（student001@demo.pcfa.jp 〜 の空き番号を使用）。
     * @return 新規作成件数
     */
    @Transactional
    public int seedAdditionalDemoStudents(int need, Long assignToCourseId) {
        if (need <= 0) return 0;
        String[] family = {"佐藤","鈴木","高橋","田中","伊藤","渡辺","山本","中村","小林","加藤",
                "吉田","山田","佐々木","山口","松本","井上","木村","林","斎藤","清水"};
        String[] given = {"太郎","花子","健太","美咲","翔","結衣","大輔","愛","誠","さくら",
                "悠斗","陽菜","蓮","結菜","颯太","莉子","湊","葵","陸","心春"};
        Random rnd = new Random();
        int created = 0;
        int seq = 1;
        while (created < need && seq < 10000) {
            String email = String.format("student%03d@demo.pcfa.jp", seq++);
            if (userRepository.existsByEmail(email)) continue;
            User user = new User();
            user.setName(family[rnd.nextInt(family.length)] + given[rnd.nextInt(given.length)]);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode("Admin1234"));
            user.setRole("STUDENT");
            user.setActive(true);
            user.setLocked(false);
            user.setLoginFailureCount(0);
            user.setPasswordResetRequired(false);
            user.setPasswordNotSet(false);
            userRepository.save(user);
            if (assignToCourseId != null) {
                Course course = courseRepository.findById(assignToCourseId).orElse(null);
                if (course != null && courseUserRepository
                        .findByCourse_IdAndUser_Id(assignToCourseId, user.getId()).isEmpty()) {
                    CourseUser cu = new CourseUser();
                    cu.setCourse(course);
                    cu.setUser(user);
                    cu.setRole("STUDENT");
                    courseUserRepository.save(cu);
                }
            }
            created++;
        }
        return created;
    }

    /** @deprecated 互換用。seedAdditionalDemoStudents を使用 */
    @Transactional
    public int seedDemoStudents(int count, Long assignToCourseId) {
        return seedAdditionalDemoStudents(count, assignToCourseId);
    }

    private void createOrUpdateUser(String name, String email, String password, String role) {
        User user = userRepository.findByEmail(email).orElse(new User());
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setActive(true);
        user.setLocked(false);
        user.setLoginFailureCount(0);
        user.setPasswordResetRequired(false);
        user.setPasswordNotSet(false);
        userRepository.save(user);
    }
}
