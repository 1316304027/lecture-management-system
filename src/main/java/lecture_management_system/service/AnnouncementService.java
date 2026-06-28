package lecture_management_system.service;

import lecture_management_system.dto.CourseStatsDto;
import lecture_management_system.entity.*;
import lecture_management_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnnouncementService {

    @Autowired private CourseAnnouncementRepository announcementRepository;
    @Autowired private CourseRepository courseRepository;

    public List<CourseAnnouncement> getByCourse(Long courseId) {
        return announcementRepository.findByCourse_IdOrderByCreatedAtDesc(courseId);
    }

    public String create(User author, Long courseId, String title, String content) {
        if (title == null || title.isBlank()) return "タイトルを入力してください";
        if (content == null || content.isBlank()) return "内容を入力してください";
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return "コースが見つかりません";

        CourseAnnouncement a = new CourseAnnouncement();
        a.setCourse(course);
        a.setAuthor(author);
        a.setTitle(title.trim());
        a.setContent(content.trim());
        a.setCreatedAt(LocalDateTime.now());
        announcementRepository.save(a);
        return null;
    }

    public CourseAnnouncement findById(Long id) {
        return announcementRepository.findById(id).orElse(null);
    }

    public String update(User author, Long announcementId, Long courseId, String title, String content) {
        CourseAnnouncement a = findById(announcementId);
        if (a == null || !a.getCourse().getId().equals(courseId)) {
            return "お知らせが見つかりません";
        }
        if (title == null || title.isBlank()) return "タイトルを入力してください";
        if (content == null || content.isBlank()) return "内容を入力してください";
        a.setTitle(title.trim());
        a.setContent(content.trim());
        announcementRepository.save(a);
        return null;
    }

    public String delete(User author, Long announcementId, Long courseId) {
        CourseAnnouncement a = findById(announcementId);
        if (a == null || !a.getCourse().getId().equals(courseId)) {
            return "お知らせが見つかりません";
        }
        announcementRepository.delete(a);
        return null;
    }

    /** 管理者によるコース変更通知（学生・講師のコースポータルにお知らせ表示） */
    public void postAdminCourseNotice(User admin, Long courseId, String title, String content) {
        if (admin == null || courseId == null || title == null || title.isBlank()) return;
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return;
        CourseAnnouncement a = new CourseAnnouncement();
        a.setCourse(course);
        a.setAuthor(admin);
        a.setTitle(title.trim());
        a.setContent(content != null ? content.trim() : "");
        a.setCreatedAt(LocalDateTime.now());
        announcementRepository.save(a);
    }

    /** 担当/受講コースの管理者お知らせ（直近） */
    public List<CourseAnnouncement> getRecentAdminNotices(List<Course> courses, int limit) {
        if (courses == null || courses.isEmpty()) return List.of();
        return courses.stream()
                .flatMap(c -> getByCourse(c.getId()).stream())
                .filter(a -> a.getAuthor() != null && "ADMIN".equals(a.getAuthor().getRole()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(limit)
                .toList();
    }

    public long countRecentAdminNotices(List<Course> courses, int withinDays) {
        if (courses == null || courses.isEmpty()) return 0;
        LocalDateTime since = LocalDateTime.now().minusDays(withinDays);
        return courses.stream()
                .flatMap(c -> getByCourse(c.getId()).stream())
                .filter(a -> a.getAuthor() != null && "ADMIN".equals(a.getAuthor().getRole()))
                .filter(a -> a.getCreatedAt() != null && a.getCreatedAt().isAfter(since))
                .count();
    }
}
