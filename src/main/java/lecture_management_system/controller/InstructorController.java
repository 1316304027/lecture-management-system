package lecture_management_system.controller;

import jakarta.servlet.http.HttpSession;
import lecture_management_system.entity.*;
import lecture_management_system.repository.SubmissionRepository;
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
import java.time.LocalDateTime;
import java.util.List;

/**
 * 講師機能Controller / Instructor Feature Controller
 *
 * 講師ダッシュボード・教材管理・課題管理・提出物確認・出欠確認を管理する。
 * Manages instructor dashboard, material management, assignment management,
 * submission review, and attendance confirmation.
 *
 * ================================================================
 * 【変更点③】evaluate エンドポイントにスコアパラメータを追加
 * [Change ③] Added score parameter to the evaluate endpoint
 *
 * 講師がコメントに加えてスコア（0〜100点）も入力・保存できるようにした。
 * Instructors can now input and save a score (0-100) along with a comment.
 * ================================================================
 */
@Controller
public class InstructorController {

    @Autowired private CourseService courseService;
    @Autowired private MaterialService materialService;
    @Autowired private AssignmentService assignmentService;
    @Autowired private SubmissionService submissionService;
    @Autowired private AttendanceService attendanceService;
    @Autowired private CourseScheduleService courseScheduleService;
    @Autowired private SubmissionRepository submissionRepository;

    // ===================== ダッシュボード（SCR-101）/ Dashboard =====================

    /**
     * 講師ホーム（GET /instructor/home）
     * 担当コース一覧を表示する。
     * Displays list of courses the instructor is assigned to.
     */
    @GetMapping("/instructor/home")
    public String home(HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";

        // 担当コース一覧（course_users WHERE role=INSTRUCTOR）
        List<Course> courses = courseService.getInstructorCourses(loginUser.getId());
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("courseList", courses);
        model.addAttribute("isProxy", session.getAttribute("adminUser") != null);
        return "instructor-home";
    }

    // ===================== 教材管理（SCR-102）/ Material Management =====================

    /**
     * 教材管理画面（GET /instructor/materials?courseId=X）
     * 担当コースの教材一覧を表示する（公開・非公開全件）。
     * Displays all materials for the course (both published and unpublished).
     */
    @GetMapping("/instructor/materials")
    public String materials(
            @RequestParam Long courseId,
            HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("course", courseService.findById(courseId));
        model.addAttribute("materialList", materialService.findByCourseId(courseId));
        return "instructor-materials";
    }

    /**
     * 教材アップロード処理（POST /instructor/materials）
     * Material upload (POST /instructor/materials)
     *
     * multipart/form-data でPDFを受け取る。
     * サイズ制限は application.yaml の spring.servlet.multipart で設定済み。
     * Receives PDF as multipart/form-data.
     * Size limit is configured in application.yaml under spring.servlet.multipart.
     */
    @PostMapping("/instructor/materials")
    public String uploadMaterial(
            @RequestParam Long courseId,
            @RequestParam String title,
            @RequestParam(defaultValue = "false") boolean published,
            @RequestParam MultipartFile file,
            HttpSession session, Model model) throws IOException {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";

        String error = materialService.uploadMaterial(courseId, title, published, file, loginUser);

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("course", courseService.findById(courseId));
        model.addAttribute("materialList", materialService.findByCourseId(courseId));
        if (error != null) {
            model.addAttribute("errorMessage", error);
        } else {
            model.addAttribute("message", "教材を登録しました");
        }
        return "instructor-materials";
    }

    /**
     * 教材公開状態切替（POST /instructor/materials/{id}/toggle）
     * Toggle material published state
     */
    @PostMapping("/instructor/materials/{id}/toggle")
    public String toggleMaterial(
            @PathVariable Long id,
            @RequestParam Long courseId) {
        materialService.togglePublished(id);
        return "redirect:/instructor/materials?courseId=" + courseId;
    }

    /**
     * 教材削除（POST /instructor/materials/{id}/delete）
     * Delete material
     */
    @PostMapping("/instructor/materials/{id}/delete")
    public String deleteMaterial(
            @PathVariable Long id,
            @RequestParam Long courseId) {
        materialService.deleteMaterial(id);
        return "redirect:/instructor/materials?courseId=" + courseId;
    }

    // ===================== 課題管理（SCR-103）/ Assignment Management =====================

    /**
     * 課題管理画面（GET /instructor/assignments?courseId=X）
     * Assignment management screen
     */
    @GetMapping("/instructor/assignments")
    public String assignments(
            @RequestParam Long courseId,
            HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("course", courseService.findById(courseId));
        model.addAttribute("assignmentList", assignmentService.findByCourseId(courseId));
        return "instructor-assignments";
    }

    /**
     * 課題作成処理（POST /instructor/assignments）
     * deadline は "yyyy-MM-ddTHH:mm" 形式で受け取る。
     * Create assignment. deadline format: "yyyy-MM-ddTHH:mm"
     */
    @PostMapping("/instructor/assignments")
    public String createAssignment(
            @RequestParam Long courseId,
            @RequestParam String title,
            @RequestParam String deadline,
            @RequestParam(defaultValue = "false") boolean published,
            @RequestParam(required = false) MultipartFile file,
            HttpSession session, Model model) throws IOException {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";

        LocalDateTime deadlineDt = LocalDateTime.parse(deadline);
        String error = assignmentService.createAssignment(
                courseId, title, deadlineDt, published, file);

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("course", courseService.findById(courseId));
        model.addAttribute("assignmentList", assignmentService.findByCourseId(courseId));
        if (error != null) {
            model.addAttribute("errorMessage", error);
        } else {
            model.addAttribute("message", "課題を作成しました");
        }
        return "instructor-assignments";
    }

    /**
     * 課題公開状態切替（POST /instructor/assignments/{id}/toggle）
     * Toggle assignment published state
     */
    @PostMapping("/instructor/assignments/{id}/toggle")
    public String toggleAssignment(
            @PathVariable Long id,
            @RequestParam Long courseId) {
        assignmentService.togglePublished(id);
        return "redirect:/instructor/assignments?courseId=" + courseId;
    }

    /**
     * 課題削除（POST /instructor/assignments/{id}/delete）
     * 【新規追加】1件の課題を削除する。
     */
    @PostMapping("/instructor/assignments/{id}/delete")
    public String deleteAssignment(
            @PathVariable Long id,
            @RequestParam Long courseId) {
        assignmentService.deleteAssignment(id);
        return "redirect:/instructor/assignments?courseId=" + courseId;
    }

    /**
     * 全課題削除（POST /instructor/assignments/delete-all）
     * 【新規追加】コースの課題を全件削除する。
     */
    @PostMapping("/instructor/assignments/delete-all")
    public String deleteAllAssignments(@RequestParam Long courseId) {
        assignmentService.deleteAllByCourseId(courseId);
        return "redirect:/instructor/assignments?courseId=" + courseId;
    }

    // ===================== 提出物確認（SCR-104）/ Submission Review =====================

    /**
     * 提出物確認画面（GET /instructor/submissions?courseId=X）
     * Submission review screen. Shows assignments and their submissions.
     */
    @GetMapping("/instructor/submissions")
    public String submissions(
            @RequestParam Long courseId,
            @RequestParam(required = false) Long assignmentId,
            HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";

        List<Assignment> assignments = assignmentService.findByCourseId(courseId);
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("course", courseService.findById(courseId));
        model.addAttribute("assignmentList", assignments);

        // 課題選択時は提出一覧を表示 / Show submission list when assignment is selected
        if (assignmentId != null) {
            model.addAttribute("selectedAssignment",
                    assignmentService.findById(assignmentId));
            model.addAttribute("submissionList",
                    submissionService.getByAssignment(assignmentId));
            // コース受講生で未提出者も表示するため学生一覧も渡す
            // Pass student list to also show non-submitting students
            model.addAttribute("studentList",
                    courseService.getStudents(courseId));
        }
        return "instructor-submissions";
    }

    /**
     * 【変更点③】評価コメント＋スコア保存（POST /instructor/submissions/{id}/evaluate）
     * [Change ③] Save evaluation comment + score
     *
     * コメントに加えてスコア（0〜100）も受け取り、一緒に保存する。
     * Receives score (0-100) in addition to comment and saves both.
     *
     * @param id           提出ID / Submission ID
     * @param comment      評価コメント / Evaluation comment
     * @param score        評価スコア 0〜100（任意）/ Score 0-100 (optional)
     * @param courseId     リダイレクト用コースID / Course ID for redirect
     * @param assignmentId リダイレクト用課題ID / Assignment ID for redirect
     */
    @PostMapping("/instructor/submissions/{id}/evaluate")
    public String evaluate(
            @PathVariable Long id,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) Integer score,
            @RequestParam Long courseId,
            @RequestParam Long assignmentId) {
        // コメントとスコアを同時に保存 / Save comment and score together
        submissionService.saveEvaluation(id, comment, score);
        return "redirect:/instructor/submissions?courseId=" + courseId
                + "&assignmentId=" + assignmentId;
    }

    /**
     * 提出物PDFダウンロード（GET /instructor/submissions/{id}/download）
     * 【S3対応】ローカルファイルではなくS3署名付きURLにリダイレクトする。
     */
    @GetMapping("/instructor/submissions/{id}/download")
    public String downloadSubmission(@PathVariable Long id) {
        Submission submission = submissionService.findById(id);
        if (submission == null) return "redirect:/instructor/home";
        String url = submissionService.getDownloadUrl(submission.getStoredFileName());
        return "redirect:" + url;
    }

    // ===================== 出欠確認（SCR-105）/ Attendance Confirmation =====================

    /**
     * 出欠確認画面（GET /instructor/attendance?courseId=X）
     * Attendance confirmation screen. Shows attendance rate per student.
     */
    @GetMapping("/instructor/attendance")
    public String attendance(
            @RequestParam Long courseId,
            @RequestParam(required = false) Long studentId,
            HttpSession session, Model model) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return "redirect:/login";

        List<User> students = courseService.getStudents(courseId);
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("course", courseService.findById(courseId));
        model.addAttribute("studentList", students);

        // 各学生の出席率と出席回数をMapに格納
        // Store attendance rate and count for each student in Maps
        java.util.Map<Long, Double> rateMap = new java.util.HashMap<>();
        java.util.Map<Long, Long> countMap = new java.util.HashMap<>();
        long totalLessons = courseScheduleService.getSchedules(courseId).size();
        for (User s : students) {
            rateMap.put(s.getId(),
                    attendanceService.calculateRate(s.getId(), courseId));
            countMap.put(s.getId(),
                    (long) attendanceService.getHistory(s.getId(), courseId).size());
        }
        model.addAttribute("rateMap", rateMap);
        model.addAttribute("countMap", countMap);
        model.addAttribute("totalLessons", totalLessons);

        // 学生詳細選択時は出席履歴も表示 / Show attendance history when student is selected
        if (studentId != null) {
            students.stream().filter(s -> s.getId().equals(studentId))
                    .findFirst().ifPresent(s -> {
                        model.addAttribute("selectedStudent", s);
                        model.addAttribute("detailHistory",
                                attendanceService.getHistory(studentId, courseId));
                    });
        }
        return "instructor-attendance";
    }

    /** セッションからログインユーザーを取得する / Get login user from session */
    private User getLoginUser(HttpSession session) {
        return (User) session.getAttribute("loginUser");
    }
}
