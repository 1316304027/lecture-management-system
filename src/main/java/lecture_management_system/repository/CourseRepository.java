package lecture_management_system.repository;

import lecture_management_system.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * コースRepository
 * coursesテーブルのDB操作を担当する。
 *
 * コースと講師・学生の紐付けは course_users テーブルで管理するため、
 * このRepositoryは基本的なCRUDのみ提供する。
 * コース別のユーザー取得は CourseUserRepository を使用する。
 */
public interface CourseRepository extends JpaRepository<Course, Long> {
    // JpaRepositoryの findAll(), findById(), save(), deleteById() で十分
}
