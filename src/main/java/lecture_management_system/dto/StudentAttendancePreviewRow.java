package lecture_management_system.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 実績レポート：受講者1名の出席明細（プレビュー・Excel出力用） */
@Data
public class StudentAttendancePreviewRow {
    private LocalDate lessonDate;
    private String lessonTimeLabel;
    private String status;
    private LocalDateTime checkInAt;
    private boolean futureLesson;
}
