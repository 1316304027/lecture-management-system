package lecture_management_system.controller;

import jakarta.servlet.http.HttpSession;
import lecture_management_system.entity.User;
import lecture_management_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

/**
 * ログインController（API-001, API-002）
 *
 * 【ログイン処理フロー（設計書4.1, 5.2.1）】
 * 1. メールアドレスでユーザー検索
 * 2. lockedチェック
 * 3. BCryptパスワード照合
 * 4. 失敗時：login_failure_countインクリメント、5回でlocked=true
 * 5. 成功時：カウントリセット、セッションにユーザー情報保存
 * 6. ロール別ホーム画面へリダイレクト
 */
@Controller
public class LoginController {

    @Autowired
    private UserService userService;

    /** ログイン画面表示（GET /login） */
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    /** ログイン処理（POST /login） */
    @PostMapping("/login")
    public String loginProcess(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        // メールアドレスでユーザー検索
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            model.addAttribute("errorMessage", "メールアドレスまたはパスワードが違います");
            return "login";
        }

        User user = userOpt.get();

        // アカウント有効チェック
        if (Boolean.FALSE.equals(user.getActive())) {
            model.addAttribute("errorMessage", "このアカウントは無効です");
            return "login";
        }

        // ロックチェック
        if (Boolean.TRUE.equals(user.getLocked())) {
            model.addAttribute("errorMessage",
                    "アカウントがロックされています。管理者にお問い合わせください");
            return "login";
        }

        // BCryptパスワード照合
        if (!userService.checkPassword(password, user.getPassword())) {
            // 失敗カウントインクリメント
            user.setLoginFailureCount(user.getLoginFailureCount() + 1);
            if (user.getLoginFailureCount() >= 5) {
                user.setLocked(true); // 5回連続失敗でロック
            }
            userService.save(user);
            model.addAttribute("errorMessage", "メールアドレスまたはパスワードが違います");
            return "login";
        }

        // 認証成功：カウントリセット、セッション設定
        user.setLoginFailureCount(0);
        userService.save(user);
        session.setAttribute("loginUser", user);

        // ロール別ホーム画面へリダイレクト
        return switch (user.getRole()) {
            case "ADMIN"      -> "redirect:/admin/home";
            case "INSTRUCTOR" -> "redirect:/instructor/home";
            default           -> "redirect:/student/home";
        };
    }

    /** ログアウト処理（GET /logout） */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}