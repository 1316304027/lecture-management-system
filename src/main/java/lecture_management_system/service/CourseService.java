package lecture_management_system.service;

import lecture_management_system.entity.*;
import lecture_management_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

/**
 * コース業務Service
 * コースCRUDおよびコースへの講師・学生割当を担当する。
 *
 * ★設計の核心★
 * 管理者がコースを作成し、course_usersテーブルで講師・学生を紐付ける。
 * 講師は自分が INSTRUCTOR として登録されたコースのみ管理できる。
 * 学生は自分が STUDENT として登録されたコースのみ閲覧できる。
 */
@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseUserRepository courseUserRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private lecture_management_system.repository.CourseScheduleRepository courseScheduleRepository;

    @Autowired
    private lecture_management_system.repository.AssignmentRepository assignmentRepository;

    @Autowired
    private lecture_management_system.repository.SubmissionRepository submissionRepository;

    @Autowired
    private lecture_management_system.repository.MaterialRepository materialRepository;

    // ===================== コースCRUD =====================

    /** 全コース一覧取得（管理者用） */
    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    /** コース詳細取得 */
    public Course findById(Long id) {
        return courseRepository.findById(id).orElse(null);
    }

    /**
     * コース新規作成（管理者SCR-203から使用）
     * コース情報のみ保存する。講師・学生割当は別途 assignInstructor/assignStudent で行う。
     */
    public Course createCourse(String name, String description) {
        Course course = new Course();
        course.setName(name);
        course.setDescription(description);
        return courseRepository.save(course);
    }

    /**
     * コース更新（管理者SCR-203から使用）
     */
    public void updateCourse(Long id, String name, String description) {
        Course course = courseRepository.findById(id).orElse(null);
        if (course != null) {
            course.setName(name);
            course.setDescription(description);
            courseRepository.save(course);
        }
    }

    /**
     * コース削除（管理者SCR-203から使用）
     * course_users, course_schedules も一緒に削除される（CascadeまたはDB制約による）。
     */
    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    /**
     * コース削除（関連データを先に全削除してからコースを削除）
     * 【修正】外部キー制約エラーによるページ崩壊を防ぐ。
     * 削除順序：submissions → assignments → attendances → schedules → course_users → course
     */
    @Transactional
    public void deleteCourseWithRelations(Long courseId) {
        // 1. 提出物（submissionsはassignment経由でcourse_idを持つ）
        List<lecture_management_system.entity.Assignment> assignments =
                assignmentRepository.findByCourse_Id(courseId);
        for (lecture_management_system.entity.Assignment a : assignments) {
            submissionRepository.deleteAll(
                submissionRepository.findByAssignment_IdOrderBySubmittedAtDesc(a.getId())
            );
        }
        // 2. 課題
        assignmentRepository.deleteAll(assignments);

        // 3. 教材
        materialRepository.deleteAll(materialRepository.findByCourse_Id(courseId));

        // 4. 出席
        attendanceRepository.deleteAll(attendanceRepository.findByCourse_Id(courseId));

        // 5. 授業日
        courseScheduleRepository.deleteAll(
                courseScheduleRepository.findByCourse_IdOrderByLessonDateAsc(courseId));

        // 6. コース所属
        courseUserRepository.deleteAll(courseUserRepository.findByCourse_Id(courseId));

        // 7. コース本体
        courseRepository.deleteById(courseId);
    }

    // ===================== 講師・学生割当 =====================

    /**
     * 講師をコースに割り当てる（管理者SCR-203から使用）
     * 既に割当済みの場合はスキップする。
     */
    public void assignInstructor(Long courseId, Long userId) {
        if (!courseUserRepository.existsByCourse_IdAndUser_Id(courseId, userId)) {
            Course course = courseRepository.findById(courseId).orElse(null);
            User user = userRepository.findById(userId).orElse(null);
            if (course != null && user != null) {
                CourseUser cu = new CourseUser();
                cu.setCourse(course);
                cu.setUser(user);
                cu.setRole("INSTRUCTOR");
                courseUserRepository.save(cu);
            }
        }
    }

    /**
     * 学生をコースに割り当てる（管理者SCR-203から使用）
     * 既に割当済みの場合はスキップする。
     */
    public void assignStudent(Long courseId, Long userId) {
        if (!courseUserRepository.existsByCourse_IdAndUser_Id(courseId, userId)) {
            Course course = courseRepository.findById(courseId).orElse(null);
            User user = userRepository.findById(userId).orElse(null);
            if (course != null && user != null) {
                CourseUser cu = new CourseUser();
                cu.setCourse(course);
                cu.setUser(user);
                cu.setRole("STUDENT");
                courseUserRepository.save(cu);
            }
        }
    }

    /**
     * コースからユーザーを解除する（管理者SCR-203から使用）
     */
    public void removeUserFromCourse(Long courseId, Long userId) {
        courseUserRepository.findByCourse_IdAndUser_Id(courseId, userId)
                .ifPresent(courseUserRepository::delete);
    }

    // ===================== コース所属取得 =====================

    /**
     * 講師の担当コース一覧取得
     * course_users で role=INSTRUCTOR のコースを返す。
     */
    public List<Course> getInstructorCourses(Long userId) {
        return courseUserRepository.findByUser_IdAndRole(userId, "INSTRUCTOR")
                .stream()
                .map(CourseUser::getCourse)
                .collect(Collectors.toList());
    }

    /**
     * 学生の受講コース一覧取得
     * course_users で role=STUDENT のコースを返す。
     */
    public List<Course> getStudentCourses(Long userId) {
        return courseUserRepository.findByUser_IdAndRole(userId, "STUDENT")
                .stream()
                .map(CourseUser::getCourse)
                .collect(Collectors.toList());
    }

    /**
     * コースに所属する講師一覧取得（コース管理画面での表示に使用）
     */
    public List<User> getInstructors(Long courseId) {
        return courseUserRepository.findByCourse_IdAndRole(courseId, "INSTRUCTOR")
                .stream()
                .map(CourseUser::getUser)
                .collect(Collectors.toList());
    }

    /**
     * コースに所属する学生一覧取得（コース管理画面での表示に使用）
     */
    public List<User> getStudents(Long courseId) {
        return courseUserRepository.findByCourse_IdAndRole(courseId, "STUDENT")
                .stream()
                .map(CourseUser::getUser)
                .collect(Collectors.toList());
    }
}
