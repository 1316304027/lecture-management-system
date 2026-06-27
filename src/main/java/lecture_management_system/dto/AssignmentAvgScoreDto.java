package lecture_management_system.dto;

import lombok.Data;

/** 課題ごとのコース平均点（実績レポート用） */
@Data
public class AssignmentAvgScoreDto {
    private Long assignmentId;
    private String title;
    private long enrolledCount;
    private long submittedCount;
    private long gradedCount;
    private double submissionRate;
    private Double averageScore;

    public void calcSubmissionRate() {
        if (enrolledCount <= 0) {
            submissionRate = 0;
        } else {
            submissionRate = Math.round(submittedCount * 1000.0 / enrolledCount) / 10.0;
        }
    }
}
