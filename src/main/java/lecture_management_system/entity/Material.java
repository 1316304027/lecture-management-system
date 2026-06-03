package lecture_management_system.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 教材エンティティ
 * materials テーブルと対応する。
 *
 * 講師が教材アップロード画面（SCR-102）からPDFを登録する。
 * published フラグで受講者への公開/非公開を制御する。
 *
 * ★旧コードからの修正点★
 * created_by（作成者ID）が欠落していたため追加。
 * 講師は自分が作成した教材のみ操作可能にするため必須。
 */
@Entity
@Table(name = "materials")
@Data
public class Material {

    /**
     * 教材ID（主キー、自動採番）
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
     * 教材タイトル（必須、100文字以内）
     */
    @Column(nullable = false, length = 255)
    private String title;

    /**
     * ストレージ上のファイル名
     * アップロード時にシステムが一意のファイル名を生成して保存する
     * 例：「{timestamp}_{originalName}.pdf」
     */
    @Column(nullable = false, length = 255)
    private String storedFileName;

    /**
     * 公開フラグ
     * true  → 受講者の教材一覧画面（SCR-004）に表示される
     * false → 受講者には非表示（講師・管理者のみ確認可）
     */
    @Column(nullable = false)
    private Boolean published = false;

    /**
     * 作成者（created_by 外部キー）
     * ★旧コードに欠けていた重要なフィールド★
     * 講師権限チェックに使用：自分が作成した教材のみ編集可能。
     */
    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
}


