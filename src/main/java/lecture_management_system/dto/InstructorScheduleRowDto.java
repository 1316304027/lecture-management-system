package lecture_management_system.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

/** 講師ホーム用：担当コースの授業スケジュール行 */
@Data
public class InstructorScheduleRowDto {
    private Long courseId;
    private String courseName;
    private LocalDate lessonDate;
    private LocalTime startTime;
    private LocalTime endTime;
    /** today / upcoming / past */
    private String timing;
}
