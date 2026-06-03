package lecture_management_system.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * ユーザーエンティティ
 * usersテーブルと対応する。
 * ロールは ADMIN / INSTRUCTOR / STUDENT の3種類。
 * アカウントロック機能あり（連続5回ログイン失敗でlocked=true）。
 */
@Entity
@Table(name = "users")
@Data
public class User {

    /**
     * ユーザーID（主キー、自動採番）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ユーザー名（表示名）
     */
    private String name;

    /**
     * メールアドレス（ログインIDとして使用、一意制約あり）
     */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * パスワード（BCryptハッシュ化して保存）
     */
    @Column(nullable = false, length = 255)
    private String password;

    /**
     * ロール（ADMIN / INSTRUCTOR / STUDENT）
     * Spring Securityでの権限判定に使用する。
     */
    @Column(nullable = false, length = 20)
    private String role;

    /**
     * アカウント有効フラグ
     * false の場合はログイン不可（論理削除的に使用）
     */
    @Column(nullable = false)
    private Boolean active = true;

    /**
     * アカウントロックフラグ
     * ログイン連続失敗5回でtrueになる。
     * 解除は管理者のみ可能（falseに戻す）。
     */
    @Column(nullable = false)
    private Boolean locked = false;

    /**
     * ログイン失敗回数
     * 成功時は0にリセット。5回到達でlockedをtrueにする。
     */
    @Column(nullable = false)
    private Integer loginFailureCount = 0;
}
