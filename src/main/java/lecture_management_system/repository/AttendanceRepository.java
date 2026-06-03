package lecture_management_system.repository;

import lecture_management_system.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    /** 重複登録チェック */
    Optional<Attendance> findByStudent_IdAndCourse_IdAndDate(
            Long studentId, Long courseId, LocalDate date);

    /** 学生の出席履歴（日付降順） */
    List<Attendance> findByStudent_IdAndCourse_IdOrderByDateDesc(
            Long studentId, Long courseId);

    /** コース全体の出席一覧（講師・管理者用） */
    List<Attendance> findByCourse_Id(Long courseId);

    /** 学生の出席回数カウント（出席率計算の分子） */
    long countByStudent_IdAndCourse_Id(Long studentId, Long courseId);

    /**
     * 学生の全出席データ取得（ユーザー削除時の一括削除用）
     * course_id に関係なく student_id で全件取得する。
     */
    List<Attendance> findByStudent_Id(Long studentId);
}

