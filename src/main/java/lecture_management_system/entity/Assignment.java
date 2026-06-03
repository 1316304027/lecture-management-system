package lecture_management_system.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 課題エンティティ
 * assignments テーブルと対応する。
 *
 * 講師が課題作成画面（SCR-103）から課題を作成する。
 * published フラグで受講者への公開/非公開を制御する。
 * deadline を過ぎた場合、受講者は提出不可になる。
 */
@Entity
@Table(name = "assignments")
@Data
public class Assignment {

    /**
     * 課題ID（主キー、自動採番）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属コース（course_id 外部キー）
     */
    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * 課題タイトル（必須）
     */
    @Column(nullable = false, length = 255)
    private String title;

    /**
     * 課題ファイルのストレージ上のファイル名（PDF）
     * 講師がアップロードした課題説明PDFのファイル名。
     */
    @Column(nullable = false, length = 255)
    private String storedFileName;

    /**
     * 公開フラグ
     * true  → 受講者の課題提出画面（SCR-005）に表示される
     * false → 受講者には非表示
     */
    @Column(nullable = false)
    private Boolean published = false;

    /**
     * 提出期限（タイムスタンプ）
     * この日時を過ぎると受講者は提出不可。
     * サービス層で LocalDateTime.now().isAfter(deadline) で判定する。
     */
    @Column(nullable = false)
    private LocalDateTime deadline;
}
