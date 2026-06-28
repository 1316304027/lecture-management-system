package lecture_management_system.config;

import jakarta.servlet.http.HttpSession;
import lecture_management_system.entity.Course;
import lecture_management_system.entity.User;
import lecture_management_system.service.AnnouncementService;
import lecture_management_system.service.CourseService;
import lecture_management_system.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * 受講者・講師ポータル用の共通モデル
 */
@ControllerAdvice
public class PortalLayoutAdvice {

    @Autowired
    private ProfileService profileService;
    @Autowired
    private CourseService courseService;
    @Autowired
    private AnnouncementService announcementService;

    @ModelAttribute("isProxy")
    public boolean isProxy(HttpSession session) {
        return session.getAttribute("adminUser") != null;
    }

    @ModelAttribute("proxyAdminName")
    public String proxyAdminName(HttpSession session) {
        User admin = (User) session.getAttribute("adminUser");
        return admin != null ? admin.getName() : null;
    }

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

    /** 管理者がコースを更新した際のお知らせ件数（30日以内） */
    @ModelAttribute("adminNoticeCount")
    public long adminNoticeCount(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) return 0;
        List<Course> courses = switch (loginUser.getRole()) {
            case "INSTRUCTOR" -> courseService.getInstructorCourses(loginUser.getId());
            case "STUDENT" -> courseService.getStudentCourses(loginUser.getId());
            default -> List.of();
        };
        return announcementService.countRecentAdminNotices(courses, 30);
    }
}
