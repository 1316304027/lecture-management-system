package lecture_management_system.service;

import lecture_management_system.entity.*;
import lecture_management_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 授業日管理Service
 *
 * 【追加機能】
 *  - 授業日登録時に授業時間帯（startTime / endTime）を保存
 *  - 一括登録：開始日〜終了日の指定曜日を一括で登録
 */
@Service
public class CourseScheduleService {

    @Autowired private CourseScheduleRepository courseScheduleRepository;
    @Autowired private CourseRepository courseRepository;

    /** コースの授業日一覧取得（昇順） */
    public List<CourseSchedule> getSchedules(Long courseId) {
        return courseScheduleRepository.findByCourse_IdOrderByLessonDateAsc(courseId);
    }

    /**
     * 授業日1件追加（時間帯付き）
     * startTime / endTime は null 可（未設定 = 終日受付）
     */
    public String addSchedule(Long courseId, LocalDate lessonDate,
                               LocalTime startTime, LocalTime endTime) {
        if (courseScheduleRepository
                .findByCourse_IdAndLessonDate(courseId, lessonDate).isPresent()) {
            return "既に同じ授業日が登録されています";
        }
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return "コースが見つかりません";

        CourseSchedule schedule = new CourseSchedule();
        schedule.setCourse(course);
        schedule.setLessonDate(lessonDate);
        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);
        courseScheduleRepository.save(schedule);
        return null;
    }

    /** 後方互換：時間帯なし版（既存コードから呼ばれる場合）*/
    public String addSchedule(Long courseId, LocalDate lessonDate) {
        return addSchedule(courseId, lessonDate, null, null);
    }

    /**
     * 一括授業日登録
     *
     * 開始日〜終了日の範囲で指定した曜日（複数可）の日付を全て登録する。
     * 重複している日はスキップして残りを登録し続ける。
     *
     * @param courseId   対象コースID
     * @param rangeFrom  開始日
     * @param rangeTo    終了日
     * @param dayOfWeeks 登録する曜日のリスト（例: [MONDAY, WEDNESDAY]）
     * @param startTime  授業開始時刻（null = 未設定）
     * @param endTime    授業終了時刻（null = 未設定）
     * @return 登録件数
     */
    @Transactional
    public int addScheduleBulk(Long courseId, LocalDate rangeFrom, LocalDate rangeTo,
                                List<DayOfWeek> dayOfWeeks,
                                LocalTime startTime, LocalTime endTime) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return 0;

        List<CourseSchedule> toSave = new ArrayList<>();
        LocalDate current = rangeFrom;
        while (!current.isAfter(rangeTo)) {
            if (dayOfWeeks.contains(current.getDayOfWeek())) {
                final LocalDate finalCurrent = current;
                boolean exists = courseScheduleRepository
                        .findByCourse_IdAndLessonDate(courseId, finalCurrent).isPresent();
                if (!exists) {
                    CourseSchedule s = new CourseSchedule();
                    s.setCourse(course);
                    s.setLessonDate(finalCurrent);
                    s.setStartTime(startTime);
                    s.setEndTime(endTime);
                    toSave.add(s);
                }
            }
            current = current.plusDays(1);
        }
        courseScheduleRepository.saveAll(toSave);
        return toSave.size();
    }

    /** 授業日削除 */
    public void deleteSchedule(Long scheduleId) {
        courseScheduleRepository.deleteById(scheduleId);
    }
}
