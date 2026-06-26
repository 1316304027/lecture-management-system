package lecture_management_system.controller;

import jakarta.servlet.http.HttpSession;
import lecture_management_system.dto.PasswordOperationResult;
import lecture_management_system.entity.Assignment;
import lecture_management_system.entity.Attendance;
import lecture_management_system.entity.Submission;
import lecture_management_system.entity.CourseSchedule;
import lecture_management_system.entity.User;
import lecture_management_system.entity.Course;
import lecture_management_system.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import lecture_management_system.dto.StudentReportDto;
import lecture_management_system.dto.StudentAttendancePreviewRow;
import lecture_management_system.dto.StudentAssignmentPreviewRow;
import lecture_management_system.repository.AssignmentRepository;
import lecture_management_system.repository.SubmissionRepository;
import lecture_management_system.repository.AttendanceRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AdminController {

    @Autowired private UserService userService;
    @Autowired private CourseService courseService;
    @Autowired private CourseScheduleService courseScheduleService;
    @Autowired private AttendanceService attendanceService;
    @Autowired private ReportService reportService;
    @Autowired private ProfileService profileService;
    @Autowired private AnnouncementService announcementService;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private AttendanceRepository attendanceRepository;

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
        model.addAttribute("studentCount", userService.countStudents());
        return "admin-home";
    }

    // ===================== ユーザー管理（SCR-202）=====================

    @GetMapping("/admin/accounts")
    public String accounts(HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        model.addAttribute("loginUser", loginUser);
        List<User> users = userService.findAllForAdminList();
        model.addAttribute("userList", users);
        model.addAttribute("avatarUrlMap", profileService.buildAvatarUrlMap(users));
        return "admin-accounts";
    }

    @GetMapping("/admin/accounts/{userId}/profile")
    public String userProfile(
            @PathVariable Long userId,
            @RequestParam(required = false) String returnTo,
            HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        User user = userService.findById(userId);
        if (user == null) return "redirect:/admin/accounts";
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("user", user);
        model.addAttribute("avatarUrl", profileService.getAvatarUrl(user));
        model.addAttribute("courseList", coursesForUser(user));
        if (returnTo != null && returnTo.startsWith("/admin/")) {
            model.addAttribute("returnUrl", returnTo);
            model.addAttribute("returnLabel",
                    returnTo.contains("reports") ? "実績レポート" : "ユーザー管理");
        } else {
            model.addAttribute("returnUrl", "/admin/accounts");
            model.addAttribute("returnLabel", "ユーザー管理");
        }
        return "admin-user-profile";
    }

    private List<Course> coursesForUser(User user) {
        if (user == null) return List.of();
        return switch (user.getRole()) {
            case "STUDENT" -> courseService.getStudentCourses(user.getId());
            case "INSTRUCTOR" -> courseService.getInstructorCourses(user.getId());
            default -> List.of();
        };
    }

    @PostMapping("/admin/accounts/create")
    public String createUser(
            @RequestParam String name, @RequestParam String email,
            @RequestParam String role,
            HttpSession session, RedirectAttributes redirectAttributes) {
        if (getLoginUser(session) == null) return "redirect:/login";
        PasswordOperationResult result = userService.createUser(name, email, role);
        if (!result.success()) {
            redirectAttributes.addFlashAttribute("errorMessage", result.error());
            if (result.devSetupLink() != null) {
                redirectAttributes.addFlashAttribute("devSetupLink", result.devSetupLink());
            }
        } else if (result.emailSent()) {
            redirectAttributes.addFlashAttribute("message",
                    "ユーザーを作成しました。パスワード設定メールを AWS SES で送信しました：" + result.email());
            redirectAttributes.addFlashAttribute("emailSentTo", result.email());
        }
        return "redirect:/admin/accounts";
    }

    @PostMapping("/admin/accounts/update/{id}")
    public String updateUser(
            @PathVariable Long id, @RequestParam String name,
            @RequestParam String email,
            @RequestParam String role,
            HttpSession session, RedirectAttributes redirectAttributes) {
        if (getLoginUser(session) == null) return "redirect:/login";
        String error = userService.updateUser(id, name, email, role);
        if (error != null) redirectAttributes.addFlashAttribute("errorMessage", error);
        else redirectAttributes.addFlashAttribute("message", "ユーザーを更新しました");
        return "redirect:/admin/accounts";
    }

    @PostMapping("/admin/accounts/{id}/reset-password")
    public String resetPassword(
            @PathVariable Long id,
            HttpSession session, RedirectAttributes redirectAttributes) {
        if (getLoginUser(session) == null) return "redirect:/login";
        PasswordOperationResult result = userService.resetPasswordByAdmin(id);
        if (!result.success()) {
            redirectAttributes.addFlashAttribute("errorMessage", result.error());
            if (result.devSetupLink() != null) {
                redirectAttributes.addFlashAttribute("devSetupLink", result.devSetupLink());
            }
        } else if (result.emailSent()) {
            redirectAttributes.addFlashAttribute("message",
                    "パスワードをリセットしました。設定メールを AWS SES で送信しました：" + result.email());
            redirectAttributes.addFlashAttribute("emailSentTo", result.email());
        }
        return "redirect:/admin/accounts";
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

    /**
     * 【1回だけ】デモ用受講者を RDS に投入（再起動では実行されない）
     */
    @GetMapping("/admin/setup/seed-students")
    public String seedStudentsPage(HttpSession session, Model model) {
        if (getLoginUser(session) == null) return "redirect:/login";
        long n = userService.countStudents();
        model.addAttribute("studentCount", n);
        model.addAttribute("needCount", Math.max(0, 93 - n));
        model.addAttribute("alreadyDone", n >= 93);
        return "admin-seed-students";
    }

    @PostMapping("/admin/setup/seed-students")
    public String seedStudentsExecute(HttpSession session, RedirectAttributes redirectAttributes) {
        if (getLoginUser(session) == null) return "redirect:/login";
        long before = userService.countStudents();
        if (before >= 93) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "受講者は既に " + before + " 名います。追加は行いません。");
            return "redirect:/admin/setup/seed-students";
        }
        Long courseId = courseService.findAll().stream()
                .map(Course::getId).min(Long::compareTo).orElse(null);
        int added = userService.seedStudentsToTargetOnce(93, courseId);
        redirectAttributes.addFlashAttribute("message",
                "今回 " + added + " 名をデータベース（RDS）に追加しました。受講者合計 "
                        + (before + added) + " 名です。");
        return "redirect:/admin/accounts";
    }

    // ===================== コース管理（SCR-203）=====================

    @GetMapping("/admin/courses")
    public String courses(
            @RequestParam(required = false) Long courseId,
            HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("courseList", courseService.findAll().stream()
                .sorted(Comparator.comparing(Course::getId))
                .toList());
        if (courseId != null) {
            model.addAttribute("selectedCourse", courseService.findById(courseId));
            model.addAttribute("courseInstructors", courseService.getInstructors(courseId));
            model.addAttribute("courseStudents", courseService.getStudents(courseId));
            model.addAttribute("schedules", courseScheduleService.getSchedules(courseId));
            Set<Long> assignedInstructorIds = courseService.getInstructors(courseId).stream()
                    .map(User::getId).collect(Collectors.toSet());
            Set<Long> assignedStudentIds = courseService.getStudents(courseId).stream()
                    .map(User::getId).collect(Collectors.toSet());
            model.addAttribute("instructorCandidates", userService.findByRole("INSTRUCTOR").stream()
                    .filter(u -> !assignedInstructorIds.contains(u.getId())).toList());
            model.addAttribute("studentCandidates", userService.findByRole("STUDENT").stream()
                    .filter(u -> !assignedStudentIds.contains(u.getId())).toList());
        } else {
            model.addAttribute("instructorCandidates", userService.findByRole("INSTRUCTOR"));
            model.addAttribute("studentCandidates", userService.findByRole("STUDENT"));
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
        User admin = getLoginUser(session);
        if (admin == null) return "redirect:/login";
        Course before = courseService.findById(id);
        String desc = description != null ? description : "";
        courseService.updateCourse(id, name, desc);
        if (before != null && (!name.equals(before.getName())
                || !desc.equals(before.getDescription() != null ? before.getDescription() : ""))) {
            announcementService.postAdminCourseNotice(admin, id,
                    "【管理者】コース情報が更新されました",
                    "コース「" + name + "」の名称または説明が変更されました。学生・講師はコースポータルのお知らせをご確認ください。");
        }
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
            @RequestParam(required = false) String startHour,
            @RequestParam(required = false) String startMinute,
            @RequestParam(required = false) String endHour,
            @RequestParam(required = false) String endMinute,
            HttpSession session) {
        User admin = getLoginUser(session);
        if (admin == null) return "redirect:/login";
        java.time.LocalTime st = parseOptionalTime(startHour, startMinute);
        java.time.LocalTime et = parseOptionalTime(endHour, endMinute);
        courseScheduleService.addSchedule(courseId, LocalDate.parse(lessonDate), st, et);
        announcementService.postAdminCourseNotice(admin, courseId,
                "【管理者】授業日が追加されました",
                "授業日 " + lessonDate + " が追加されました。出席登録・スケジュールをご確認ください。");
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
            @RequestParam(required = false) String startHour,
            @RequestParam(required = false) String startMinute,
            @RequestParam(required = false) String endHour,
            @RequestParam(required = false) String endMinute,
            HttpSession session) {
        User admin = getLoginUser(session);
        if (admin == null) return "redirect:/login";
        if (dayOfWeeks == null || dayOfWeeks.isEmpty())
            return "redirect:/admin/courses?courseId=" + courseId;

        java.time.LocalTime st = parseOptionalTime(startHour, startMinute);
        java.time.LocalTime et = parseOptionalTime(endHour, endMinute);

        java.util.List<java.time.DayOfWeek> dows = dayOfWeeks.stream()
                .map(java.time.DayOfWeek::valueOf)
                .toList();

        int added = courseScheduleService.addScheduleBulk(
                courseId,
                LocalDate.parse(rangeFrom),
                LocalDate.parse(rangeTo),
                dows, st, et);
        if (added > 0) {
            announcementService.postAdminCourseNotice(admin, courseId,
                    "【管理者】授業日が一括追加されました",
                    rangeFrom + " 〜 " + rangeTo + " の範囲で " + added + " 件の授業日が追加されました。");
        }
        return "redirect:/admin/courses?courseId=" + courseId;
    }

    @PostMapping("/admin/courses/{courseId}/schedule/delete/{scheduleId}")
    public String deleteSchedule(
            @PathVariable Long courseId, @PathVariable Long scheduleId,
            HttpSession session) {
        User admin = getLoginUser(session);
        if (admin == null) return "redirect:/login";
        CourseSchedule schedule = courseScheduleService.findById(scheduleId);
        courseScheduleService.deleteSchedule(scheduleId);
        if (schedule != null) {
            announcementService.postAdminCourseNotice(admin, courseId,
                    "【管理者】授業日が削除されました",
                    "授業日 " + schedule.getLessonDate() + " が削除されました。");
        }
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
            Course selected = courseService.findById(courseId);
            if (selected == null) {
                model.addAttribute("courseNotFound", true);
                model.addAttribute("requestedCourseId", courseId);
                return "admin-reports";
            }
            model.addAttribute("selectedCourse", selected);
            List<StudentReportDto> reportList = reportService.getCourseReport(courseId);
            model.addAttribute("reportList", reportList != null ? reportList : List.of());
            model.addAttribute("lessonStats", reportService.getLessonAttendanceStats(courseId));
            if (reportList != null && !reportList.isEmpty()) {
                double avgAtt = reportList.stream()
                        .filter(r -> r.getTotalLessons() > 0)
                        .mapToDouble(StudentReportDto::getAttendanceRate)
                        .average().orElse(0);
                double avgSub = reportList.stream()
                        .filter(r -> r.getTotalAssignments() > 0)
                        .mapToDouble(StudentReportDto::getSubmissionRate)
                        .average().orElse(0);
                model.addAttribute("avgAttendanceRate", Math.round(avgAtt * 10) / 10.0);
                model.addAttribute("avgSubmissionRate", Math.round(avgSub * 10) / 10.0);
            }
        }
        return "admin-reports";
    }

    @GetMapping("/admin/reports/student-attendance")
    public String previewStudentAttendance(
            @RequestParam Long courseId,
            @RequestParam Long studentId,
            HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        Course course = courseService.findById(courseId);
        User student = userService.findById(studentId);
        if (course == null || student == null) return "redirect:/admin/reports?courseId=" + courseId;
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("course", course);
        model.addAttribute("student", student);
        model.addAttribute("rows", reportService.getStudentAttendancePreview(courseId, studentId));
        model.addAttribute("returnUrl", "/admin/reports?courseId=" + courseId);
        return "admin-report-preview-attendance";
    }

    @GetMapping("/admin/reports/student-assignments")
    public String previewStudentAssignments(
            @RequestParam Long courseId,
            @RequestParam Long studentId,
            HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        Course course = courseService.findById(courseId);
        User student = userService.findById(studentId);
        if (course == null || student == null) return "redirect:/admin/reports?courseId=" + courseId;
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("course", course);
        model.addAttribute("student", student);
        model.addAttribute("rows", reportService.getStudentAssignmentPreview(courseId, studentId));
        model.addAttribute("returnUrl", "/admin/reports?courseId=" + courseId);
        return "admin-report-preview-assignments";
    }

    @GetMapping("/admin/reports/export")
    public void exportReportsCsv(
            @RequestParam Long courseId,
            @RequestParam String type,
            @RequestParam Long studentId,
            HttpSession session,
            HttpServletResponse response) throws IOException {
        if (getLoginUser(session) == null) {
            response.sendError(401);
            return;
        }
        Course course = courseService.findById(courseId);
        String courseName = course != null ? course.getName() : "course";
        User student = userService.findById(studentId);
        if (student == null) {
            response.sendError(404);
            return;
        }
        if ("assignments".equals(type)) {
            exportStudentAssignmentsCsv(courseId, courseName, student, response);
        } else {
            exportStudentAttendanceCsv(courseId, courseName, student, response);
        }
    }

    private static final DateTimeFormatter CSV_DT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    private void exportStudentAttendanceCsv(Long courseId, String courseName, User student,
                                            HttpServletResponse response) throws IOException {
        List<StudentAttendancePreviewRow> rows = reportService.getStudentAttendancePreview(courseId, student.getId());
        PrintWriter w = prepareCsv(response,
                "出席一覧_" + student.getName() + "_コース" + courseId + ".csv");
        w.println("コース," + csv(courseName));
        w.println("氏名," + csv(student.getName()));
        w.println("メール," + csv(student.getEmail()));
        w.println();
        w.println("授業日,授業時間,出席状態,打刻日時");
        for (StudentAttendancePreviewRow r : rows) {
            String checkIn = r.getCheckInAt() != null ? r.getCheckInAt().format(CSV_DT) : "";
            w.printf("%s,%s,%s,%s%n",
                    r.getLessonDate(), csv(r.getLessonTimeLabel()), r.getStatus(), checkIn);
        }
        w.flush();
    }

    private void exportStudentAssignmentsCsv(Long courseId, String courseName, User student,
                                             HttpServletResponse response) throws IOException {
        List<StudentAssignmentPreviewRow> rows = reportService.getStudentAssignmentPreview(courseId, student.getId());
        PrintWriter w = prepareCsv(response,
                "課題一覧_" + student.getName() + "_コース" + courseId + ".csv");
        w.println("コース," + csv(courseName));
        w.println("氏名," + csv(student.getName()));
        w.println("メール," + csv(student.getEmail()));
        w.println();
        w.println("課題名,提出期限,提出状態,スコア,提出日時");
        for (StudentAssignmentPreviewRow r : rows) {
            String deadline = r.getDeadline() != null ? r.getDeadline().format(CSV_DT) : "";
            String submitted = r.getSubmittedAt() != null ? r.getSubmittedAt().format(CSV_DT) : "";
            String score = r.getScore() != null ? r.getScore() + "点" : "—";
            w.printf("%s,%s,%s,%s,%s%n",
                    csv(r.getTitle()), deadline, r.getStatus(), score, submitted);
        }
        w.flush();
    }

    private PrintWriter prepareCsv(HttpServletResponse response, String filename) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        PrintWriter w = response.getWriter();
        w.write('\uFEFF');
        return w;
    }

    private String csv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    private java.time.LocalTime parseOptionalTime(String hour, String minute) {
        if (hour == null || hour.isBlank()) return null;
        int h = Integer.parseInt(hour);
        int m = (minute != null && !minute.isBlank()) ? Integer.parseInt(minute) : 0;
        return java.time.LocalTime.of(h, m);
    }

    private User getLoginUser(HttpSession session) {
        return (User) session.getAttribute("loginUser");
    }
}
