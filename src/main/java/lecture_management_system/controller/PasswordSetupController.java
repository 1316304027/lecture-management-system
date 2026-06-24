package lecture_management_system.controller;

import jakarta.servlet.http.HttpSession;
import lecture_management_system.entity.User;
import lecture_management_system.service.PasswordResetService;
import lecture_management_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * メールリンク経由のパスワード設定（AWS SES 連携）
 * 管理者はパスワードを知らない。ユーザー本人のみが設定する。
 */
@Controller
public class PasswordSetupController {

    @Autowired private UserService userService;
    @Autowired private PasswordResetService passwordResetService;

    @GetMapping("/setup-password")
    public String setupPage(@RequestParam(required = false) String token, Model model) {
        if (token == null || token.isBlank()) {
            model.addAttribute("errorMessage", "無効なリンクです");
            return "setup-password";
        }
        User user = passwordResetService.validateToken(token);
        if (user == null) {
            model.addAttribute("errorMessage",
                    "リンクが無効または期限切れです。管理者に「PWリセット」を依頼してください。");
            return "setup-password";
        }
        model.addAttribute("token", token);
        model.addAttribute("userName", user.getName());
        model.addAttribute("userEmail", user.getEmail());
        return "setup-password";
    }

    @PostMapping("/setup-password")
    public String setupSubmit(
            @RequestParam String token,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model) {
        User user = passwordResetService.validateToken(token);
        if (user == null) {
            model.addAttribute("errorMessage", "リンクが無効または期限切れです");
            return "setup-password";
        }
        String err = userService.setupPasswordViaToken(token, newPassword, confirmPassword);
        if (err != null) {
            model.addAttribute("errorMessage", err);
            model.addAttribute("token", token);
            model.addAttribute("userName", user.getName());
            model.addAttribute("userEmail", user.getEmail());
            return "setup-password";
        }
        model.addAttribute("successMessage", "パスワードを設定しました。ログイン画面からお入りください。");
        return "setup-password";
    }
}
