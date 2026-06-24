package lecture_management_system.repository;

import lecture_management_system.entity.CourseAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourseAnnouncementRepository extends JpaRepository<CourseAnnouncement, Long> {

    List<CourseAnnouncement> findByCourse_IdOrderByCreatedAtDesc(Long courseId);

    long countByCourse_Id(Long courseId);
}
