package lecture_management_system.dto;

import lombok.Data;

/** 課題ごとのコース平均点（実績レポート用） */
@Data
public class AssignmentAvgScoreDto {
    private Long assignmentId;
    private String title;
    private long submittedCount;
    private long gradedCount;
    private Double averageScore;
}
