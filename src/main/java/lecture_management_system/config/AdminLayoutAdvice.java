package lecture_management_system.config;

import jakarta.servlet.http.HttpSession;
import lecture_management_system.entity.User;
import lecture_management_system.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 管理者レイアウト用の共通モデル属性（ナビ头像など）
 */
@ControllerAdvice
public class AdminLayoutAdvice {

    @Autowired
    private ProfileService profileService;

    @ModelAttribute("adminNavAvatarUrl")
    public String adminNavAvatarUrl(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) return null;
        User adminUser = (User) session.getAttribute("adminUser");
        User display = adminUser != null ? adminUser : loginUser;
        if (!"ADMIN".equals(display.getRole()) && adminUser == null) {
            return null;
        }
        try {
            return profileService.getAvatarUrl(display);
        } catch (Exception e) {
            return null;
        }
    }
}
