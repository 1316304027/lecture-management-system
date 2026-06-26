package lecture_management_system.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

/** 授業日ごとのコース全体出席率（実績レポート用） */
@Data
public class LessonAttendanceStatDto {
    private LocalDate lessonDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String lessonTimeLabel;
    private long presentCount;
    private long enrolledCount;
    private double attendanceRate;

    public void calcRate() {
        if (enrolledCount == 0) {
            attendanceRate = 0;
        } else {
            attendanceRate = Math.round((double) presentCount / enrolledCount * 1000) / 10.0;
        }
    }
}
