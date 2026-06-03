package lecture_management_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 課題提出エンティティ（submissions テーブル）
 * Submission Entity (submissions table)
 *
 * 受講者が課題提出画面（SCR-005）でPDFをアップロードした時に作成される。
 * Created when a student uploads a PDF on the assignment submission screen (SCR-005).
 *
 * 講師が提出物確認画面（SCR-104）で評価コメントとスコアを入力・保存できる。
 * Instructors can enter evaluation comments and scores on the submission review screen (SCR-104).
 *
 * ================================================================
 * 【変更点③】評価スコアフィールド (score) を追加
 * [Change ③] Added evaluation score field (score)
 *
 * - score: 0〜100の整数点数。未評価はnull。
 * - score: Integer 0-100. null if not yet evaluated.
 * - 管理者実績レポートで学生ごとの平均スコアを表示するために使用。
 * - Used in admin reports to display average score per student.
 * ================================================================
 */
@Entity
@Table(name = "submissions")
@Data
public class Submission {

    /** 提出ID（PK、自動採番） / Submission ID (PK, auto-increment) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 対象課題（assignment_id FK） / Target assignment (FK) */
    @ManyToOne
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    /** 提出した受講者（student_id FK） / Submitting student (FK) */
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /** ストレージ上のファイル名（タイムスタンプ+拡張子で生成） / Stored file name */
    @Column(nullable = false, length = 255)
    private String storedFileName;

    /** 元のファイル名（ダウンロード時のContent-Dispositionに使用） / Original file name */
    @Column(length = 255)
    private String originalFileName;

    /** 提出日時 / Submission timestamp */
    @Column(nullable = false)
    private LocalDateTime submittedAt;

    /** 講師の評価コメント（500文字以内）。未評価はnull / Instructor comment (max 500 chars, null if unreviewed) */
    @Column(length = 500)
    private String evaluationComment;

    /**
     * 【変更点③】講師の評価スコア（0〜100点）。未評価はnull。
     * [Change ③] Instructor evaluation score (0-100). null if not yet evaluated.
     *
     * score が設定されると評価済みとみなす。
     * DBのカラム名は "score"。
     */
    @Column(name = "score")
    private Integer score;

    /** 評価日時。未評価はnull / Evaluation timestamp. null if unreviewed */
    private LocalDateTime evaluatedAt;
}
