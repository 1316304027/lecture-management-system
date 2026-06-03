package lecture_management_system.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * コースエンティティ
 * coursesテーブルと対応する。
 *
 * ★重要★
 * 旧コードにあった teacher_id や course_students の @ManyToMany は
 * 設計書の構造と異なるため完全に削除した。
 *
 * 講師・受講者とコースの紐付けは course_users テーブル（CourseUser エンティティ）
 * で管理する。管理者がコース作成後に講師・学生を割り当てる設計。
 */
@Entity
@Table(name = "courses")
@Data
public class Course {

    /**
     * コースID（主キー、自動採番）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * コース名（必須）
     */
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * コース説明（任意）
     */
    @Column(columnDefinition = "TEXT")
    private String description;
}
