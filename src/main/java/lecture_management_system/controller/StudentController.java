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
    @Autowired private CourseScheduleService courseScheduleService; // ← 追加

    // ===================== ホーム（SCR-002）=====================

    @GetMapping("/student/home")
    public String home(HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("courseList", courseService.getStudentCourses(loginUser.getId()));
        model.addAttribute("isProxy", session.getAttribute("adminUser") != null);
        return "student-home";
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
    @ResponseBody
    public ResponseEntity<Resource> downloadMaterial(
            @PathVariable Long id, HttpSession session) throws IOException {
        if (getLoginUser(session) == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Material material = materialService.findById(id);
        if (material == null) return ResponseEntity.notFound().build();
        Path path = materialService.getFilePath(material.getStoredFileName());
        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''"
                        + URLEncoder.encode(material.getTitle() + ".pdf", StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
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
