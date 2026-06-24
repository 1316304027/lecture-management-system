package lecture_management_system.controller;

import jakarta.servlet.http.HttpSession;
import lecture_management_system.entity.*;
import lecture_management_system.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lecture_management_system.dto.AvatarPresignResult;
import lecture_management_system.dto.CourseStatsDto;
import lecture_management_system.dto.PresignUploadResponse;
import lecture_management_system.dto.UploadPrepareResult;

/**
 * 学生機能Controller / Student Feature Controller
 *
 * 【修正】attendancePage() と registerAttendance() に
 *   courseScheduleService.getSchedules(courseId) → "scheduleList"
 *   を追加する。
 *
 *   student-attendance.html が scheduleList を th:each でループするため
 *   これがないと NullPointerException でページがクラッシュする。
 *   Adding scheduleList to model prevents NullPointerException crash in
 *   student-attendance.html which loops over it with th:each.
 */
@Controller
public class StudentController {

    @Autowired private CourseService courseService;
    @Autowired private AttendanceService attendanceService;
    @Autowired private MaterialService materialService;
    @Autowired private AssignmentService assignmentService;
    @Autowired private SubmissionService submissionService;
    @Autowired private CourseScheduleService courseScheduleService;
    @Autowired private ProfileService profileService;
    @Autowired private ChatService chatService;
    @Autowired private StudentDashboardService studentDashboardService;
    @Autowired private AnnouncementService announcementService;

    // ===================== ホーム（SCR-002）=====================

    @GetMapping("/student/home")
    public String home(HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        loginUser = profileService.refreshUser(loginUser);
        session.setAttribute("loginUser", loginUser);
        List<Course> courses = courseService.getStudentCourses(loginUser.getId());
        Map<Long, CourseStatsDto> courseStatsMap = studentDashboardService.buildCourseStatsMap(loginUser, courses);

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("avatarUrl", profileService.getAvatarUrl(loginUser));
        model.addAttribute("courseList", courses);
        model.addAttribute("courseStatsMap", courseStatsMap);
        model.addAttribute("chatUnreadMap", chatService.buildUnreadMap(loginUser, courses));
        model.addAttribute("totalPendingAssignments", studentDashboardService.totalPendingAssignments(courseStatsMap));
        model.addAttribute("totalUnreadChat", studentDashboardService.totalUnreadChat(courseStatsMap));
        model.addAttribute("avgAttendanceRate", studentDashboardService.averageAttendanceRate(courseStatsMap));
        model.addAttribute("isProxy", session.getAttribute("adminUser") != null);
        return "student-home";
    }

    /** コースポータル（お知らせ + クイックリンク） */
    @GetMapping("/student/course")
    public String coursePortal(@RequestParam Long courseId, HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";

        Course course = courseService.findById(courseId);
        if (course == null) return "redirect:/student/home";

        Map<Long, CourseStatsDto> stats = studentDashboardService.buildCourseStatsMap(
                loginUser, List.of(course));
        CourseStatsDto courseStats = stats.get(courseId);

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("course", course);
        model.addAttribute("courseStats", courseStats);
        model.addAttribute("announcements", announcementService.getByCourse(courseId));
        model.addAttribute("materialList", materialService.findPublishedByCourseId(courseId));
        model.addAttribute("assignmentList", assignmentService.findPublishedByCourseId(courseId));
        model.addAttribute("chatUnread", chatService.countUnread(loginUser, courseId));
        return "student-course-portal";
    }

    // ===================== プロフィール（S3アバター）=====================

    @GetMapping("/student/profile")
    public String profilePage(HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        loginUser = profileService.refreshUser(loginUser);
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("avatarUrl", profileService.getAvatarUrl(loginUser));
        return "student-profile";
    }

    @GetMapping("/student/profile/presign-avatar")
    @ResponseBody
    public ResponseEntity<?> presignAvatar(@RequestParam String fileName, HttpSession session) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        AvatarPresignResult r = profileService.prepareAvatarUpload(loginUser, fileName);
        if (r == null) return ResponseEntity.badRequest().body(Map.of("error", "invalid_file"));
        return ResponseEntity.ok(Map.of("uploadUrl", r.uploadUrl(), "s3Key", r.s3Key()));
    }

    @PostMapping("/student/profile")
    public String saveProfile(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String profileBio,
            @RequestParam(required = false) String avatarS3Key,
            HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        profileService.updateProfile(loginUser, phone, profileBio);
        if (avatarS3Key != null && !avatarS3Key.isBlank()) {
            profileService.saveAvatarKey(loginUser, avatarS3Key);
        }
        loginUser = profileService.refreshUser(loginUser);
        session.setAttribute("loginUser", loginUser);
        model.addAttribute("message", "プロフィールを保存しました");
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("avatarUrl", profileService.getAvatarUrl(loginUser));
        return "student-profile";
    }

    @GetMapping("/student/profile/view")
    public String profileView(HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        loginUser = profileService.refreshUser(loginUser);
        model.addAttribute("user", loginUser);
        model.addAttribute("avatarUrl", profileService.getAvatarUrl(loginUser));
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("courseList", courseService.getStudentCourses(loginUser.getId()));
        return "student-profile-view";
    }

    // ===================== 出席登録（SCR-003）=====================

    /**
     * 出席登録画面（GET /student/attendance?courseId=X）
     *
     * 【修正】scheduleList を追加
     * student-attendance.html で全授業日×出席状態の一覧を表示するために
     * courseScheduleService.getSchedules(courseId) を "scheduleList" として渡す。
     * この1行がないとページがクラッシュする。
     */
    @GetMapping("/student/attendance")
    public String attendancePage(@RequestParam Long courseId,
                                 HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("course", courseService.findById(courseId));
        model.addAttribute("isTodayLesson", attendanceService.isTodayLesson(courseId));
        model.addAttribute("todaySchedule", attendanceService.getTodaySchedule(courseId));
        model.addAttribute("isTodayAttended",
                attendanceService.isTodayAttended(loginUser.getId(), courseId));
        model.addAttribute("attendanceRate",
                attendanceService.calculateRate(loginUser.getId(), courseId));
        List<Attendance> historyList = attendanceService.getHistory(loginUser.getId(), courseId);
        model.addAttribute("historyList", historyList);
        // 【修正】インライン式バグ回避：Map<LocalDate, Attendance> で渡す
        Map<LocalDate, Attendance> attendanceMap = new HashMap<>();
        for (Attendance a : historyList) { attendanceMap.put(a.getDate(), a); }
        model.addAttribute("attendanceMap", attendanceMap);
        // 【修正】全授業日スケジュールを追加（これがないとページクラッシュ）
        model.addAttribute("scheduleList",
                courseScheduleService.getSchedules(courseId));
        return "student-attendance";
    }

    /**
     * 出席登録処理（POST /student/attendance）
     *
     * 【修正】POST後の再表示でも scheduleList が必要なので追加
     */
    @PostMapping("/student/attendance")
    public String registerAttendance(@RequestParam Long courseId,
                                     HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";

        String result = attendanceService.registerAttendance(loginUser, courseId);
        String message = switch (result) {
            case "success"   -> "出席登録が完了しました ✅";
            case "already"   -> "本日は既に出席登録済みです";
            case "no_lesson" -> "本日は授業が実施されていません";
            case "too_early" -> "⏰ まだ出席受付時間前です（授業開始30分前から受付開始）";
            case "too_late"  -> "⏰ 出席受付時間が終了しました（授業開始30分後まで受付）";
            default          -> "エラーが発生しました";
        };

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("course", courseService.findById(courseId));
        model.addAttribute("message", message);
        model.addAttribute("isTodayLesson", attendanceService.isTodayLesson(courseId));
        model.addAttribute("todaySchedule", attendanceService.getTodaySchedule(courseId));
        model.addAttribute("isTodayAttended",
                attendanceService.isTodayAttended(loginUser.getId(), courseId));
        model.addAttribute("attendanceRate",
                attendanceService.calculateRate(loginUser.getId(), courseId));
        List<Attendance> historyListPost = attendanceService.getHistory(loginUser.getId(), courseId);
        model.addAttribute("historyList", historyListPost);
        // 【修正】インライン式バグ回避：Map<LocalDate, Attendance> で渡す
        Map<LocalDate, Attendance> attendanceMapPost = new HashMap<>();
        for (Attendance a : historyListPost) { attendanceMapPost.put(a.getDate(), a); }
        model.addAttribute("attendanceMap", attendanceMapPost);
        // 【修正】POST後も scheduleList を追加
        model.addAttribute("scheduleList",
                courseScheduleService.getSchedules(courseId));
        return "student-attendance";
    }

    // ===================== 教材（SCR-004）=====================

    @GetMapping("/student/materials")
    public String materials(@RequestParam Long courseId,
                            HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("course", courseService.findById(courseId));
        model.addAttribute("materialList",
                materialService.findPublishedByCourseId(courseId));
        return "student-materials";
    }

    @GetMapping("/student/materials/{id}/download")
    public String downloadMaterial(@PathVariable Long id, HttpSession session) {
        if (getLoginUser(session) == null) return "redirect:/login";
        Material material = materialService.findById(id);
        if (material == null) return "redirect:/student/home";
        String url = materialService.getDownloadUrl(material.getStoredFileName());
        return "redirect:" + url;
    }

    // ===================== 課題提出（SCR-005）=====================

    @GetMapping("/student/assignments")
    public String assignments(@RequestParam Long courseId,
                              HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("course", courseService.findById(courseId));
        model.addAttribute("assignmentList",
                assignmentService.findPublishedByCourseId(courseId));
        model.addAttribute("mySubmissions",
                submissionService.getByStudent(loginUser.getId()));
        return "student-assignments";
    }

    /** 項番43: PUT Presigned URL 発行 */
    @GetMapping("/student/assignments/{assignmentId}/presign-upload")
    @ResponseBody
    public ResponseEntity<?> presignUpload(
            @PathVariable Long assignmentId,
            @RequestParam Long courseId,
            @RequestParam String originalFileName,
            HttpSession session) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return ResponseEntity.status(401).body(Map.of("error", "unauthorized"));
        Assignment assignment = assignmentService.findById(assignmentId);
        if (assignment == null) return ResponseEntity.badRequest().body(Map.of("error", "not_found"));
        UploadPrepareResult r = submissionService.prepareDirectUpload(loginUser, assignment, originalFileName);
        if (!r.isOk()) return ResponseEntity.badRequest().body(Map.of("error", r.errorCode()));
        PresignUploadResponse p = r.response();
        return ResponseEntity.ok(Map.of("uploadUrl", p.uploadUrl(), "s3Key", p.s3Key()));
    }

    @PostMapping("/student/assignments/submit")
    public String submitAssignment(
            @RequestParam Long assignmentId,
            @RequestParam Long courseId,
            @RequestParam MultipartFile file,
            HttpSession session, Model model) throws IOException {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        Assignment assignment = assignmentService.findById(assignmentId);
        String result = submissionService.submit(loginUser, assignment, file);
        String message = switch (result) {
            case "success"   -> "課題提出が完了しました ✅";
            case "already"   -> "既に提出済みです";
            case "deadline"  -> "提出期限を過ぎています";
            case "pdf_only"  -> "PDF形式のみアップロード可能です";
            case "too_large" -> "ファイルサイズ上限（10MB）を超えています";
            case "empty"     -> "ファイルを選択してください";
            default          -> "エラーが発生しました";
        };
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("course", courseService.findById(courseId));
        model.addAttribute("message", message);
        model.addAttribute("assignmentList",
                assignmentService.findPublishedByCourseId(courseId));
        model.addAttribute("mySubmissions",
                submissionService.getByStudent(loginUser.getId()));
        return "student-assignments";
    }

    private User getLoginUser(HttpSession session) {
        return (User) session.getAttribute("loginUser");
    }
}
