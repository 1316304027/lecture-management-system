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
 * 【機能説明】
 * 学生・教師・管理者が使うチャット機能のController。
 *
 * 【URL設計】
 * GET  /chat?courseId=○○       → チャット画面を表示
 * POST /chat/send               → メッセージを送信
 * GET  /admin/chat              → 管理者用全メッセージ閲覧
 *
 * 【ロール別アクセス】
 * 学生・教師 → /chat でコースのチャットを見る
 * 管理者     → /admin/chat で全コースのメッセージを見る
 */
@Controller
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private CourseService courseService;

    /**
     * チャット画面表示（GET /chat）
     *
     * 処理の流れ：
     * 1. セッションからログインユーザーを取得
     * 2. コース情報とメッセージ一覧を取得
     * 3. chat.htmlに渡して表示
     *
     * @param courseId チャットを表示するコースのID
     */
    @GetMapping("/chat")
    public String chatPage(@RequestParam Long courseId,
                           HttpSession session,
                           Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";

        // コース情報とメッセージ一覧をモデルに追加
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("course", courseService.findById(courseId));
        model.addAttribute("messages", chatService.getMessages(courseId));
        model.addAttribute("courseId", courseId);
        return "chat";
    }

    /**
     * メッセージ送信（POST /chat/send）
     *
     * 処理の流れ：
     * 1. セッションからログインユーザーを取得
     * 2. ChatService.sendMessage()でRDSに保存
     * 3. チャット画面にリダイレクト（画面を再表示）
     *
     * @param courseId コースID
     * @param content  メッセージ本文
     */
    @PostMapping("/chat/send")
    public String sendMessage(@RequestParam Long courseId,
                              @RequestParam String content,
                              HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";

        // ChatServiceでRDSに保存 + CloudWatchログ出力
        chatService.sendMessage(loginUser, courseId, content);

        // 送信後はチャット画面に戻る
        return "redirect:/chat?courseId=" + courseId;
    }

    /**
     * 管理者用全メッセージ閲覧（GET /admin/chat）
     *
     * 管理者は全コースのメッセージを見ることができる。
     * メッセージは新しい順で表示される。
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
}
