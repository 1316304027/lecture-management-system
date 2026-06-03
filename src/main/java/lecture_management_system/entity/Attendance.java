package lecture_management_system.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 出席エンティティ
 * attendances テーブルと対応する。
 *
 * 学生が出席登録画面（SCR-003）で「出席登録ボタン」を押した時に作成される。
 *
 * 【出席登録の前提条件チェック（AttendanceServiceで行う）】
 * 1. course_schedules に今日の授業日が存在するか確認
 * 2. 同一学生・同一コース・同一日の重複登録がないか確認
 * 上記2点を満たした場合のみ登録を許可する。
 *
 * ★旧コードからの修正点★
 * course_id（所属コース）が欠落していたため追加。
 * 出席はコースに紐付くため必須。
 */
@Entity
@Table(name = "attendances")
@Data
public class Attendance {

    /**
     * 出席ID（主キー、自動採番）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 出席した受講者（student_id 外部キー）
     */
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /**
     * 出席対象コース（course_id 外部キー）
     * ★旧コードに欠けていた重要なフィールド★
     * どのコースの出席かを記録するために必須。
     */
    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * 出席日（yyyy-MM-dd）
     * course_schedules の lesson_date と照合して授業実施確認を行う。
     */
    @Column(nullable = false)
    private LocalDate date;

    /**
     * 出席登録日時（ボタンを押した正確な時刻）
     */
    @Column(nullable = false)
    private LocalDateTime attendedAt;
}
