package lecture_management_system.config;

import jakarta.servlet.http.HttpSession;
import lecture_management_system.entity.User;
import lecture_management_system.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 受講者・講師ポータル用の共通モデル
 */
@ControllerAdvice
public class PortalLayoutAdvice {

    @Autowired
    private ProfileService profileService;

    @ModelAttribute("isProxy")
    public boolean isProxy(HttpSession session) {
        return session.getAttribute("adminUser") != null;
    }

    @ModelAttribute("proxyAdminName")
    public String proxyAdminName(HttpSession session) {
        User admin = (User) session.getAttribute("adminUser");
        return admin != null ? admin.getName() : null;
    }

    /** 画面上に表示するユーザー（代理ログイン時は受講者/講師本人） */
    @ModelAttribute("portalUser")
    public User portalUser(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) return null;
        String role = loginUser.getRole();
        if (!"STUDENT".equals(role) && !"INSTRUCTOR".equals(role)) {
            return null;
        }
        return loginUser;
    }

    @ModelAttribute("portalNavAvatarUrl")
    public String portalNavAvatarUrl(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) return null;
        String role = loginUser.getRole();
        if (!"STUDENT".equals(role) && !"INSTRUCTOR".equals(role)) {
            return null;
        }
        try {
            return profileService.getAvatarUrl(loginUser);
        } catch (Exception e) {
            return null;
        }
    }
}
