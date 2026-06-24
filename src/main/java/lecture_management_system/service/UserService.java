package lecture_management_system.service;

import jakarta.annotation.PostConstruct;
import lecture_management_system.dto.PasswordOperationResult;
import lecture_management_system.entity.User;
import lecture_management_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private CourseUserRepository courseUserRepository;
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
        createOrUpdateUser("講師-奈良鹿丸",  "teacher@test.com",  "Admin1234", "INSTRUCTOR");
        createOrUpdateUser("受講者-渡辺勇大", "student@test.com",  "Admin1234", "STUDENT");
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
