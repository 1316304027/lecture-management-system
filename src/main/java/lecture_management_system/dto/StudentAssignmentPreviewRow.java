package lecture_management_system.dto;

import lombok.Data;
import java.time.LocalDateTime;

/** 実績レポート：受講者1名の課題明細（プレビュー・Excel出力用） */
@Data
public class StudentAssignmentPreviewRow {
    private String title;
    private LocalDateTime deadline;
    private String status;
    private Integer score;
    private LocalDateTime submittedAt;
}
