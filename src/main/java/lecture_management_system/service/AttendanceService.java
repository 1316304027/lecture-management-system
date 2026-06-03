package lecture_management_system.service;

import lecture_management_system.entity.*;
import lecture_management_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * 出席業務Service
 *
 * 【追加】出席可能時間帯チェック
 *   授業日にstartTimeが設定されている場合：
 *     出席可能 = startTime - 30分 〜 startTime + 30分
 *   startTimeが未設定の場合：終日受付（チェックなし）
 */
@Service
public class AttendanceService {

    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private CourseScheduleRepository courseScheduleRepository;
    @Autowired private CourseRepository courseRepository;

    /** 出席登録前チェック用の時間マージン（分） */
    private static final int CHECKIN_MARGIN_MINUTES = 30;

    /**
     * 出席登録（学生SCR-003から使用）
     *
     * @return "success" / "no_lesson" / "already" / "too_early" / "too_late"
     */
    public String registerAttendance(User student, Long courseId) {
        LocalDate today = LocalDate.now();
        LocalTime now   = LocalTime.now();

        // 1. 授業日チェック
        Optional<CourseSchedule> scheduleOpt =
                courseScheduleRepository.findByCourse_IdAndLessonDate(courseId, today);
        if (scheduleOpt.isEmpty()) return "no_lesson";

        // 2. 出席可能時間帯チェック（startTime 設定済みの場合のみ）
        CourseSchedule schedule = scheduleOpt.get();
        if (schedule.getStartTime() != null) {
            LocalTime openTime  = schedule.getStartTime().minusMinutes(CHECKIN_MARGIN_MINUTES);
            LocalTime closeTime = schedule.getStartTime().plusMinutes(CHECKIN_MARGIN_MINUTES);
            if (now.isBefore(openTime))  return "too_early";
            if (now.isAfter(closeTime))  return "too_late";
        }

        // 3. 重複チェック
        Optional<Attendance> existing =
                attendanceRepository.findByStudent_IdAndCourse_IdAndDate(
                        student.getId(), courseId, today);
        if (existing.isPresent()) return "already";

        // 4. 出席登録
        Course course = courseRepository.findById(courseId).orElse(null);
        Attendance attendance = new Attendance();
        attendance.setStudent(student);
        attendance.setCourse(course);
        attendance.setDate(today);
        attendance.setAttendedAt(LocalDateTime.now());
        attendanceRepository.save(attendance);
        return "success";
    }

    /** 今日の出席状態確認 */
    public boolean isTodayAttended(Long studentId, Long courseId) {
        return attendanceRepository.findByStudent_IdAndCourse_IdAndDate(
                studentId, courseId, LocalDate.now()).isPresent();
    }

    /** 今日が授業日か確認 */
    public boolean isTodayLesson(Long courseId) {
        return courseScheduleRepository
                .findByCourse_IdAndLessonDate(courseId, LocalDate.now()).isPresent();
    }

    /**
     * 本日の授業スケジュール取得（学生画面で時間帯を表示するため）
     * @return 今日の授業日エンティティ（null = 授業なし）
     */
    public CourseSchedule getTodaySchedule(Long courseId) {
        return courseScheduleRepository
                .findByCourse_IdAndLessonDate(courseId, LocalDate.now())
                .orElse(null);
    }

    /** 学生の出席履歴取得（日付降順） */
    public List<Attendance> getHistory(Long studentId, Long courseId) {
        return attendanceRepository
                .findByStudent_IdAndCourse_IdOrderByDateDesc(studentId, courseId);
    }

    /** 出席率計算 */
    public double calculateRate(Long studentId, Long courseId) {
        long total = courseScheduleRepository
                .countByCourse_IdAndLessonDateLessThanEqual(courseId, LocalDate.now());
        if (total == 0) return 0.0;
        long attended = attendanceRepository.countByStudent_IdAndCourse_Id(studentId, courseId);
        return Math.round((double) attended / total * 100 * 10) / 10.0;
    }

    /** コースの全出席データ取得 */
    public List<Attendance> getCourseAttendances(Long courseId) {
        return attendanceRepository.findByCourse_Id(courseId);
    }

    /** コースの授業日一覧取得 */
    public List<CourseSchedule> getSchedules(Long courseId) {
        return courseScheduleRepository.findByCourse_IdOrderByLessonDateAsc(courseId);
    }

    /** 出席レコードをIDで直接削除（孤立データ削除用） */
    public void deleteById(Long attendanceId) {
        attendanceRepository.deleteById(attendanceId);
    }

    /** 管理者による出席修正（User付き版） */
    public void updateAttendance(User student, Long courseId,
                                 LocalDate lessonDate, boolean attended) {
        Optional<Attendance> existing =
                attendanceRepository.findByStudent_IdAndCourse_IdAndDate(
                        student.getId(), courseId, lessonDate);
        if (attended && existing.isEmpty()) {
            Course course = courseRepository.findById(courseId).orElse(null);
            Attendance a = new Attendance();
            a.setStudent(student);
            a.setCourse(course);
            a.setDate(lessonDate);
            a.setAttendedAt(LocalDateTime.now());
            attendanceRepository.save(a);
        } else if (!attended && existing.isPresent()) {
            attendanceRepository.delete(existing.get());
        }
    }
}
