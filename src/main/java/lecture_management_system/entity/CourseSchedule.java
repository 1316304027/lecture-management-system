package lecture_management_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 授業日エンティティ
 * course_schedules テーブルと対応する。
 *
 * 【追加】授業時間帯フィールド（start_time / end_time）
 *   - nullable=true：時間未設定でも登録可能
 *   - 出席可能時間帯 = 授業開始30分前 〜 授業開始30分後
 *   - 時間未設定の場合は時間チェックをスキップ（終日受付）
 */
@Entity
@Table(name = "course_schedules")
@Data
public class CourseSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /** 授業実施日 */
    @Column(name = "lesson_date", nullable = false)
    private LocalDate lessonDate;

    /**
     * 授業開始時刻（任意）
     * 設定すると出席可能時間帯チェックが有効になる。
     */
    @Column(name = "start_time")
    private LocalTime startTime;

    /**
     * 授業終了時刻（任意）
     * 表示用。出席チェックには start_time のみ使用。
     */
    @Column(name = "end_time")
    private LocalTime endTime;
}
