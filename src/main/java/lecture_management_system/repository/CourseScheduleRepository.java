package lecture_management_system.repository;

import lecture_management_system.entity.CourseSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CourseScheduleRepository extends JpaRepository<CourseSchedule, Long> {

    /** コースの授業日一覧（昇順） */
    List<CourseSchedule> findByCourse_IdOrderByLessonDateAsc(Long courseId);

    /** 特定日の授業存在確認 */
    Optional<CourseSchedule> findByCourse_IdAndLessonDate(Long courseId, LocalDate lessonDate);

    /** 実施済み授業数カウント（出席率計算の分母） */
    long countByCourse_IdAndLessonDateLessThanEqual(Long courseId, LocalDate today);
}
