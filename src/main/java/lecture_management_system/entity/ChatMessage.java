package lecture_management_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * チャットメッセージEntity
 *
 * 【機能説明】
 * このクラスはDBの「chat_messages」テーブルと対応している。
 * 1行 = 1つのメッセージを表す。
 *
 * 【フィールド説明】
 * - id        : メッセージの連番ID（自動採番）
 * - sender    : 送信者（UserエンティティのIDで紐付け）
 * - course    : どのコースのチャットか（CourseエンティティのIDで紐付け）
 * - content   : メッセージ本文
 * - sentAt    : 送信日時（自動設定）
 *
 * 【RDS連携】
 * このEntityはRDS（PostgreSQL）のchat_messagesテーブルに保存される。
 * Spring Bootが自動でテーブルを作成する（ddl-auto=update）。
 */
@Entity
@Table(name = "chat_messages")
@Data
public class ChatMessage {

    /** メッセージID（主キー・自動採番） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 送信者
     * ManyToOne = 多くのメッセージが1人のUserに対応する
     * JoinColumn = DBのchat_messagesテーブルにsender_idカラムを作成
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    /**
     * 対象コース
     * ManyToOne = 多くのメッセージが1つのCourseに対応する
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * メッセージ本文
     * TEXT型 = 長い文章も保存できる
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 送信日時
     * メッセージ保存時に自動でセットする
     */
    @Column(nullable = false)
    private LocalDateTime sentAt;
}
