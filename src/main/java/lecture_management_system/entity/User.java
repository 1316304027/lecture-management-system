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

    /** S3上の顔写真キー（例: avatars/3/uuid.jpg） */
    @Column(name = "avatar_s3_key", length = 512)
    private String avatarS3Key;

    /** 電話番号（プロフィール） */
    @Column(length = 50)
    private String phone;

    /** 自己紹介（プロフィール） */
    @Column(name = "profile_bio", columnDefinition = "TEXT")
    private String profileBio;

    /**
     * 初回ログインまたは管理者リセット後、本人がパスワード変更するまで true。
     * 管理者はパスワードを直接設定・変更できない（リセットのみ）。
     */
    @Column(name = "password_reset_required", nullable = false)
    private Boolean passwordResetRequired = false;

    /**
     * true = 本人がメールリンクからパスワード未設定（ログイン不可）。
     * 管理者はパスワードを知らない・設定しない。
     */
    @Column(name = "password_not_set", nullable = false)
    private Boolean passwordNotSet = false;
}
