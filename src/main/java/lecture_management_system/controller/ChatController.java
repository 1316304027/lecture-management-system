package lecture_management_system.controller;

import jakarta.servlet.http.HttpSession;
import lecture_management_system.entity.*;
import lecture_management_system.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * チャット機能Controller
 *
 * 【URL設計】
 * GET  /chat?courseId=○○       → チャット画面表示（学生・教師）
 * POST /chat/send               → メッセージ送信
 * GET  /admin/chat              → 全メッセージ閲覧（管理者）
 * POST /admin/chat/delete/{id}  → メッセージ削除（管理者のみ）
 */
@Controller
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private CourseService courseService;

    /**
     * チャット画面表示（GET /chat）
     * 学生・教師がコースのチャットを見る
     */
    @GetMapping("/chat")
    public String chatPage(@RequestParam Long courseId,
                           HttpSession session,
                           Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("course", courseService.findById(courseId));
        model.addAttribute("messages", chatService.getMessages(courseId));
        model.addAttribute("courseId", courseId);
        return "chat";
    }

    /**
     * メッセージ送信（POST /chat/send）
     * RDSに保存 + CloudWatchログ出力
     */
    @PostMapping("/chat/send")
    public String sendMessage(@RequestParam Long courseId,
                              @RequestParam String content,
                              HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";

        chatService.sendMessage(loginUser, courseId, content);
        return "redirect:/chat?courseId=" + courseId;
    }

    /**
     * 管理者用全メッセージ閲覧（GET /admin/chat）
     * 全コースのメッセージを新しい順で表示
     */
    @GetMapping("/admin/chat")
    public String adminChat(HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";
        if (!"ADMIN".equals(loginUser.getRole())) return "redirect:/login";

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("messages", chatService.getAllMessages());
        return "admin-chat";
    }

    /**
     * メッセージ削除（POST /admin/chat/delete/{id}）
     * 管理者のみ使用可能
     * IDで1件削除してから管理者チャット画面に戻る
     */
    @PostMapping("/admin/chat/delete/{id}")
    public String deleteMessage(@PathVariable Long id,
                                HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";
        if (!"ADMIN".equals(loginUser.getRole())) return "redirect:/login";

        chatService.deleteMessage(id);
        return "redirect:/admin/chat";
    }
}
