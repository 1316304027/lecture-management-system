package lecture_management_system.dto;

import lombok.Data;

/** 学生ホーム・コースポータル用のコース別統計 */
@Data
public class CourseStatsDto {
    private Long courseId;
    private double attendanceRate;
    private long pendingAssignments;
    private long unreadChat;
    private long announcementCount;
}
