package lecture_management_system.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * コース所属エンティティ（★最重要★）
 * course_users テーブルと対応する。
 *
 * このエンティティが設計の核心。
 * 管理者がコースを作成した後、講師と学生をこのテーブルで紐付ける。
 *
 * role カラムで同一テーブル内に講師（INSTRUCTOR）と受講者（STUDENT）を管理する。
 *
 * 旧コードの Course.java にあった teacher_id と course_students は
 * このエンティティに完全に置き換わる。
 *
 * 【利用例】
 * - 管理者がコースに講師を割り当て → role = "INSTRUCTOR" で INSERT
 * - 管理者がコースに学生を割り当て → role = "STUDENT" で INSERT
 * - 講師が自分の担当コース一覧取得 → course_users WHERE user_id=X AND role='INSTRUCTOR'
 * - 学生が自分の受講コース一覧取得 → course_users WHERE user_id=X AND role='STUDENT'
 */
@Entity
@Table(name = "course_users")
@Data
public class CourseUser {

    /**
     * 所属ID（主キー、自動採番）
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
     * 所属ユーザー（user_id 外部キー）
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * ロール（INSTRUCTOR または STUDENT）
     * ADMIN はコース所属を持たない（全コース管理のため不要）。
     */
    @Column(nullable = false, length = 20)
    private String role;
}