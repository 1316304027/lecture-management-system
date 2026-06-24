package lecture_management_system.service;

import lecture_management_system.dto.CourseStatsDto;
import lecture_management_system.entity.Course;
import lecture_management_system.entity.User;
import lecture_management_system.repository.AssignmentRepository;
import lecture_management_system.repository.CourseAnnouncementRepository;
import lecture_management_system.repository.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudentDashboardService {

    @Autowired private AttendanceService attendanceService;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private ChatService chatService;
    @Autowired private CourseAnnouncementRepository announcementRepository;

    public Map<Long, CourseStatsDto> buildCourseStatsMap(User student, List<Course> courses) {
        Map<Long, CourseStatsDto> map = new HashMap<>();
        if (student == null || courses == null) return map;
        Map<Long, Long> unread = chatService.buildUnreadMap(student, courses);

        for (Course c : courses) {
            CourseStatsDto dto = new CourseStatsDto();
            dto.setCourseId(c.getId());
            dto.setAttendanceRate(attendanceService.calculateRate(student.getId(), c.getId()));

            long published = assignmentRepository.countByCourse_IdAndPublishedTrue(c.getId());
            long submitted = submissionRepository
                    .countByStudent_IdAndAssignment_Course_IdAndAssignment_PublishedTrue(
                            student.getId(), c.getId());
            dto.setPendingAssignments(Math.max(0, published - submitted));

            dto.setUnreadChat(unread.getOrDefault(c.getId(), 0L));
            dto.setAnnouncementCount(announcementRepository.countByCourse_Id(c.getId()));
            map.put(c.getId(), dto);
        }
        return map;
    }

    public long totalPendingAssignments(Map<Long, CourseStatsDto> stats) {
        return stats.values().stream().mapToLong(CourseStatsDto::getPendingAssignments).sum();
    }

    public long totalUnreadChat(Map<Long, CourseStatsDto> stats) {
        return stats.values().stream().mapToLong(CourseStatsDto::getUnreadChat).sum();
    }

    public double averageAttendanceRate(Map<Long, CourseStatsDto> stats) {
        if (stats.isEmpty()) return 0;
        return Math.round(stats.values().stream()
                .mapToDouble(CourseStatsDto::getAttendanceRate).average().orElse(0) * 10) / 10.0;
    }
}
