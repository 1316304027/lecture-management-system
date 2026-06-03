package lecture_management_system.controller;

import jakarta.servlet.http.HttpSession;
import lecture_management_system.entity.*;
import lecture_management_system.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AdminController {

    @Autowired private UserService userService;
    @Autowired private CourseService courseService;
    @Autowired private CourseScheduleService courseScheduleService;
    @Autowired private AttendanceService attendanceService;
    @Autowired private ReportService reportService;

    // ===================== 管理者ホーム（SCR-201）=====================

    @GetMapping("/admin/home")
    public String home(HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        model.addAttribute("loginUser", loginUser);
        User adminUser = (User) session.getAttribute("adminUser");
        model.addAttribute("realAdminUser", adminUser != null ? adminUser : loginUser);
        model.addAttribute("userCount", userService.findAll().size());
        model.addAttribute("courseCount", courseService.findAll().size());
        return "admin-home";
    }

    // ===================== ユーザー管理（SCR-202）=====================

    @GetMapping("/admin/accounts")
    public String accounts(HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("userList", userService.findAll());
        return "admin-accounts";
    }

    @PostMapping("/admin/accounts/create")
    public String createUser(
            @RequestParam String name, @RequestParam String email,
            @RequestParam String password, @RequestParam String role,
            HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        String error = userService.createUser(name, email, password, role);
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("userList", userService.findAll());
        if (error != null) model.addAttribute("errorMessage", error);
        else model.addAttribute("message", "ユーザーを作成しました");
        return "admin-accounts";
    }

    @PostMapping("/admin/accounts/update/{id}")
    public String updateUser(
            @PathVariable Long id, @RequestParam String name,
            @RequestParam String email,
            @RequestParam(required = false) String password,
            @RequestParam String role,
            HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        String error = userService.updateUser(id, name, email, password, role);
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("userList", userService.findAll());
        if (error != null) model.addAttribute("errorMessage", error);
        else model.addAttribute("message", "ユーザーを更新しました");
        return "admin-accounts";
    }

    @PostMapping("/admin/accounts/delete/{id}")
    public String deleteUser(@PathVariable Long id, HttpSession session) {
        if (getLoginUser(session) == null) return "redirect:/login";
        userService.deleteUser(id);
        return "redirect:/admin/accounts";
    }

    @PostMapping("/admin/accounts/unlock/{id}")
    public String unlockUser(@PathVariable Long id, HttpSession session) {
        if (getLoginUser(session) == null) return "redirect:/login";
        userService.unlockUser(id);
        return "redirect:/admin/accounts";
    }

    @PostMapping("/admin/proxy-login/{userId}")
    public String proxyLogin(@PathVariable Long userId, HttpSession session) {
        User adminUser = getLoginUser(session);
        if (adminUser == null) return "redirect:/login";
        User targetUser = userService.findById(userId);
        if (targetUser == null) return "redirect:/admin/accounts";
        session.setAttribute("adminUser", adminUser);
        session.setAttribute("loginUser", targetUser);
        return switch (targetUser.getRole()) {
            case "INSTRUCTOR" -> "redirect:/instructor/home";
            default           -> "redirect:/student/home";
        };
    }

    @PostMapping("/admin/restore-session")
    public String restoreSession(HttpSession session) {
        User adminUser = (User) session.getAttribute("adminUser");
        if (adminUser != null) {
            session.setAttribute("loginUser", adminUser);
            session.removeAttribute("adminUser");
        }
        return "redirect:/admin/home";
    }

    // ===================== コース管理（SCR-203）=====================

    @GetMapping("/admin/courses")
    public String courses(
            @RequestParam(required = false) Long courseId,
            HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("courseList", courseService.findAll());
        model.addAttribute("instructorCandidates", userService.findByRole("INSTRUCTOR"));
        model.addAttribute("studentCandidates", userService.findByRole("STUDENT"));
        if (courseId != null) {
            model.addAttribute("selectedCourse", courseService.findById(courseId));
            model.addAttribute("courseInstructors", courseService.getInstructors(courseId));
            model.addAttribute("courseStudents", courseService.getStudents(courseId));
            model.addAttribute("schedules", courseScheduleService.getSchedules(courseId));
        }
        return "admin-courses";
    }

    @PostMapping("/admin/courses/create")
    public String createCourse(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            HttpSession session) {
        if (getLoginUser(session) == null) return "redirect:/login";
        courseService.createCourse(name, description != null ? description : "");
        return "redirect:/admin/courses";
    }

    @PostMapping("/admin/courses/update/{id}")
    public String updateCourse(
            @PathVariable Long id, @RequestParam String name,
            @RequestParam(required = false) String description,
            HttpSession session) {
        if (getLoginUser(session) == null) return "redirect:/login";
        courseService.updateCourse(id, name, description != null ? description : "");
        return "redirect:/admin/courses?courseId=" + id;
    }

    /**
     * コース削除（SCR-203）
     * 【修正】CourseService.deleteCourseWithRelations() で関連データを先に削除してからコースを削除する。
     * 外部キー制約によるエラーを防ぐ。
     */
    @PostMapping("/admin/courses/delete/{id}")
    public String deleteCourse(@PathVariable Long id, HttpSession session) {
        if (getLoginUser(session) == null) return "redirect:/login";
        courseService.deleteCourseWithRelations(id);
        return "redirect:/admin/courses";
    }

    @PostMapping("/admin/courses/{courseId}/assign-instructor")
    public String assignInstructor(
            @PathVariable Long courseId, @RequestParam Long userId,
            HttpSession session) {
        if (getLoginUser(session) == null) return "redirect:/login";
        courseService.assignInstructor(courseId, userId);
        return "redirect:/admin/courses?courseId=" + courseId;
    }

    @PostMapping("/admin/courses/{courseId}/assign-student")
    public String assignStudent(
            @PathVariable Long courseId, @RequestParam Long userId,
            HttpSession session) {
        if (getLoginUser(session) == null) return "redirect:/login";
        courseService.assignStudent(courseId, userId);
        return "redirect:/admin/courses?courseId=" + courseId;
    }

    @PostMapping("/admin/courses/{courseId}/remove-user")
    public String removeUser(
            @PathVariable Long courseId, @RequestParam Long userId,
            HttpSession session) {
        if (getLoginUser(session) == null) return "redirect:/login";
        courseService.removeUserFromCourse(courseId, userId);
        return "redirect:/admin/courses?courseId=" + courseId;
    }

    @PostMapping("/admin/courses/{courseId}/schedule/add")
    public String addSchedule(
            @PathVariable Long courseId,
            @RequestParam String lessonDate,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpSession session) {
        if (getLoginUser(session) == null) return "redirect:/login";
        java.time.LocalTime st = (startTime != null && !startTime.isBlank())
                ? java.time.LocalTime.parse(startTime) : null;
        java.time.LocalTime et = (endTime != null && !endTime.isBlank())
                ? java.time.LocalTime.parse(endTime) : null;
        courseScheduleService.addSchedule(courseId, LocalDate.parse(lessonDate), st, et);
        return "redirect:/admin/courses?courseId=" + courseId;
    }

    /**
     * 一括授業日登録（POST /admin/courses/{courseId}/schedule/bulk-add）
     * 開始日〜終了日の指定曜日を一括登録する。
     */
    @PostMapping("/admin/courses/{courseId}/schedule/bulk-add")
    public String addScheduleBulk(
            @PathVariable Long courseId,
            @RequestParam String rangeFrom,
            @RequestParam String rangeTo,
            @RequestParam(required = false) java.util.List<String> dayOfWeeks,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpSession session) {
        if (getLoginUser(session) == null) return "redirect:/login";
        if (dayOfWeeks == null || dayOfWeeks.isEmpty())
            return "redirect:/admin/courses?courseId=" + courseId;

        java.time.LocalTime st = (startTime != null && !startTime.isBlank())
                ? java.time.LocalTime.parse(startTime) : null;
        java.time.LocalTime et = (endTime != null && !endTime.isBlank())
                ? java.time.LocalTime.parse(endTime) : null;

        java.util.List<java.time.DayOfWeek> dows = dayOfWeeks.stream()
                .map(java.time.DayOfWeek::valueOf)
                .toList();

        courseScheduleService.addScheduleBulk(
                courseId,
                LocalDate.parse(rangeFrom),
                LocalDate.parse(rangeTo),
                dows, st, et);
        return "redirect:/admin/courses?courseId=" + courseId;
    }

    @PostMapping("/admin/courses/{courseId}/schedule/delete/{scheduleId}")
    public String deleteSchedule(
            @PathVariable Long courseId, @PathVariable Long scheduleId,
            HttpSession session) {
        if (getLoginUser(session) == null) return "redirect:/login";
        courseScheduleService.deleteSchedule(scheduleId);
        return "redirect:/admin/courses?courseId=" + courseId;
    }

    // ===================== 出欠修正（SCR-204）=====================

    /**
     * 出欠修正画面（GET /admin/attendance）
     *
     * 【修正】attendanceMap を構築してテンプレートに渡す。
     * attendanceMap: Map<LocalDate, Boolean> (lessonDate → attended)
     * Thymeleaf のインライン式バグを回避するためMapを使う。
     *
     * 【修正】orphanAttendances: 授業日未登録の孤立出席データを別途取得して渡す。
     */
    @GetMapping("/admin/attendance")
    public String attendance(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String errorMessage,
            HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("courseList", courseService.findAll());

        if (message != null && !message.isBlank())
            model.addAttribute("message", message);
        if (errorMessage != null && !errorMessage.isBlank())
            model.addAttribute("errorMessage", errorMessage);

        if (courseId != null) {
            model.addAttribute("selectedCourse", courseService.findById(courseId));
            model.addAttribute("studentList", courseService.getStudents(courseId));

            if (studentId != null) {
                User selectedStudent = userService.findById(studentId);
                model.addAttribute("selectedStudent", selectedStudent);

                List<CourseSchedule> schedules = courseScheduleService.getSchedules(courseId);
                model.addAttribute("schedules", schedules);

                // 【修正】全出席データからMapを構築
                List<Attendance> attendances = attendanceService.getCourseAttendances(courseId);

                // この学生の出席日付セットを作成
                Set<LocalDate> attendedDates = attendances.stream()
                        .filter(a -> a.getStudent().getId().equals(studentId))
                        .map(Attendance::getDate)
                        .collect(Collectors.toSet());

                // attendanceMap: lessonDate → attended(true/false)
                Map<LocalDate, Boolean> attendanceMap = new HashMap<>();
                for (CourseSchedule s : schedules) {
                    attendanceMap.put(s.getLessonDate(), attendedDates.contains(s.getLessonDate()));
                }
                model.addAttribute("attendanceMap", attendanceMap);

                // 【修正】孤立レコード: 授業日に対応しないこの学生の出席データ
                Set<LocalDate> scheduleDates = schedules.stream()
                        .map(CourseSchedule::getLessonDate)
                        .collect(Collectors.toSet());

                List<Attendance> orphanAttendances = attendances.stream()
                        .filter(a -> a.getStudent().getId().equals(studentId))
                        .filter(a -> !scheduleDates.contains(a.getDate()))
                        .collect(Collectors.toList());
                model.addAttribute("orphanAttendances", orphanAttendances);
            }
        }
        return "admin-attendance";
    }

    /**
     * 出欠修正保存（POST /admin/attendance/update）
     */
    @PostMapping("/admin/attendance/update")
    public String updateAttendance(
            @RequestParam Long courseId,
            @RequestParam Long studentId,
            @RequestParam String lessonDate,
            @RequestParam(defaultValue = "false") boolean attended,
            HttpSession session) {
        if (getLoginUser(session) == null) return "redirect:/login";

        LocalDate date = LocalDate.parse(lessonDate);

        if (date.isAfter(LocalDate.now())) {
            String err = "⚠️ " + lessonDate
                    + " はまだ授業が実施されていません。本日以前の授業日のみ修正できます。";
            try {
                return "redirect:/admin/attendance?courseId=" + courseId
                        + "&studentId=" + studentId
                        + "&errorMessage=" + URLEncoder.encode(err, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return "redirect:/admin/attendance?courseId=" + courseId + "&studentId=" + studentId;
            }
        }

        User student = userService.findById(studentId);
        attendanceService.updateAttendance(student, courseId, date, attended);

        String attendedStr = attended ? "出席" : "欠席";
        String msg = "✅ " + lessonDate + " の出席状態を「" + attendedStr + "」に修正しました。";
        try {
            return "redirect:/admin/attendance?courseId=" + courseId
                    + "&studentId=" + studentId
                    + "&message=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "redirect:/admin/attendance?courseId=" + courseId + "&studentId=" + studentId;
        }
    }

    /**
     * 孤立出席レコード削除（POST /admin/attendance/delete）
     * 【新規追加】授業日に紐付かない孤立出席データをIDで直接削除する。
     */
    @PostMapping("/admin/attendance/delete")
    public String deleteAttendance(
            @RequestParam Long courseId,
            @RequestParam Long studentId,
            @RequestParam Long attendanceId,
            HttpSession session) {
        if (getLoginUser(session) == null) return "redirect:/login";
        attendanceService.deleteById(attendanceId);
        String msg = "🗑️ 孤立した出席データを削除しました。";
        try {
            return "redirect:/admin/attendance?courseId=" + courseId
                    + "&studentId=" + studentId
                    + "&message=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "redirect:/admin/attendance?courseId=" + courseId + "&studentId=" + studentId;
        }
    }

    // ===================== 実績レポート（SCR-205）=====================

    @GetMapping("/admin/reports")
    public String reports(
            @RequestParam(required = false) Long courseId,
            HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("courseList", courseService.findAll());
        if (courseId != null) {
            model.addAttribute("selectedCourse", courseService.findById(courseId));
            model.addAttribute("reportList", reportService.getCourseReport(courseId));
        }
        return "admin-reports";
    }

    private User getLoginUser(HttpSession session) {
        return (User) session.getAttribute("loginUser");
    }
}
