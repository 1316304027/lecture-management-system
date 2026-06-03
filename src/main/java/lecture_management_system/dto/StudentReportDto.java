package lecture_management_system.dto;

import lombok.Data;

/**
 * 学生実績レポートDTO / Student Achievement Report DTO
 *
 * 実績レポート画面（SCR-205）で学生ごとに集計した情報を保持する。
 * Holds aggregated info per student for the admin report screen (SCR-205).
 * Entityではなく画面表示用の集計データクラス。
 * This is a view/display data class, not a JPA entity.
 *
 * ================================================================
 * 【変更点③】平均スコアフィールド (averageScore) を追加
 * [Change ③] Added average score field (averageScore)
 *
 * 管理者の実績レポートで各学生の提出課題の平均スコアを表示するために追加。
 * Added to display average evaluation score per student in admin reports.
 * ================================================================
 */
@Data
public class StudentReportDto {

    /** 学生ID / Student ID */
    private Long studentId;

    /** 学生名 / Student name */
    private String studentName;

    /** 学生メールアドレス / Student email */
    private String studentEmail;

    /** 出席回数 / Attended lesson count */
    private long attendedCount;

    /** 実施済み授業数（lesson_date <= 今日）/ Total past lessons (lesson_date <= today) */
    private long totalLessons;

    /**
     * 出席率（%） / Attendance rate (%)
     * totalLessons が 0 の場合は 0 を返す。
     * Returns 0 when totalLessons is 0.
     */
    public double getAttendanceRate() {
        if (totalLessons == 0) return 0;
        return Math.round((double) attendedCount / totalLessons * 100 * 10) / 10.0;
    }

    /** 提出済み課題数 / Submitted assignment count */
    private long submittedCount;

    /** コース全課題数 / Total course assignment count */
    private long totalAssignments;

    /**
     * 提出率（%） / Submission rate (%)
     * totalAssignments が 0 の場合は 0 を返す。
     * Returns 0 when totalAssignments is 0.
     */
    public double getSubmissionRate() {
        if (totalAssignments == 0) return 0;
        return Math.round((double) submittedCount / totalAssignments * 100 * 10) / 10.0;
    }

    /**
     * 未提出件数 / Unsubmitted count
     * totalAssignments - submittedCount
     */
    public long getUnsubmittedCount() {
        return totalAssignments - submittedCount;
    }

    /**
     * 【変更点③】評価済み提出の平均スコア（0〜100）
     * [Change ③] Average score of evaluated submissions (0-100)
     *
     * 評価済み（スコアが入力されている）提出が1件もない場合は null。
     * null when no submissions have been evaluated (no score set).
     * ReportService で計算してセットする。
     * Calculated and set by ReportService.
     */
    private Double averageScore;
}
