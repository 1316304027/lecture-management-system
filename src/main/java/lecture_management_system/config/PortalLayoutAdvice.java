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
