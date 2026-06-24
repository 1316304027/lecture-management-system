package lecture_management_system.service;

import lecture_management_system.dto.PasswordOperationResult;
import lecture_management_system.entity.PasswordResetToken;
import lecture_management_system.entity.User;
import lecture_management_system.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * パスワード設定トークン発行 + AWS SES メール送信。
 * 管理者はパスワードを設定せず、ユーザーがメールリンクから本人設定する。
 */
@Service
public class PasswordResetService {

    @Autowired private PasswordResetTokenRepository tokenRepository;
    @Autowired private EmailService emailService;

    @Value("${app.mail.dev-show-link-on-failure:false}")
    private boolean devShowLinkOnFailure;

    private static final int TOKEN_HOURS = 24;

    /**
     * 新規作成・管理者リセット時：トークン発行して SES メール送信
     */
    @Transactional
    public PasswordOperationResult issueSetupEmail(User user) {
        String token = createToken(user);
        boolean sent = emailService.sendPasswordSetupEmail(user.getEmail(), user.getName(), token);
        if (sent) {
            return PasswordOperationResult.emailSent(user.getEmail());
        }
        if (devShowLinkOnFailure) {
            return PasswordOperationResult.emailFailed(
                    user.getEmail(), emailService.buildSetupLink(token));
        }
        return PasswordOperationResult.fail(
                "AWS SES でメールを送信できませんでした。from-email の検証と IAM 権限（ses:SendEmail）を確認してください。");
    }

    @Transactional
    public String createToken(User user) {
        tokenRepository.findByUserAndUsedFalse(user).forEach(t -> {
            t.setUsed(true);
            tokenRepository.save(t);
        });
        PasswordResetToken t = new PasswordResetToken();
        t.setUser(user);
        t.setToken(UUID.randomUUID().toString());
        t.setExpiresAt(LocalDateTime.now().plusHours(TOKEN_HOURS));
        t.setUsed(false);
        t.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(t);
        return t.getToken();
    }

    public User validateToken(String tokenStr) {
        if (tokenStr == null || tokenStr.isBlank()) return null;
        PasswordResetToken t = tokenRepository.findByTokenAndUsedFalse(tokenStr).orElse(null);
        if (t == null) return null;
        if (t.getExpiresAt().isBefore(LocalDateTime.now())) return null;
        return t.getUser();
    }

    @Transactional
    public void consumeToken(String tokenStr) {
        tokenRepository.findByTokenAndUsedFalse(tokenStr).ifPresent(t -> {
            t.setUsed(true);
            tokenRepository.save(t);
        });
    }
}
