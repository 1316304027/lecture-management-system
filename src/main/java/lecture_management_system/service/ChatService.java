package lecture_management_system.service;

import lecture_management_system.entity.*;
import lecture_management_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * チャット業務Service
 *
 * 【機能説明】
 * ・メッセージ送信 → RDSに保存 + CloudWatchログ出力（Lambda連携）
 * ・メッセージ取得 → コース別・全件
 * ・メッセージ削除 → 管理者のみ使用可能
 *
 * 【Lambda連携の仕組み】
 * sendMessage()でlogger.info()を呼ぶと
 * CloudWatchにログが送られる。
 * このログをLambdaのトリガーにすることで
 * 「メッセージが送信された」イベントを検知できる。
 */
@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private CourseRepository courseRepository;

    /**
     * メッセージを送信する
     * 1. バリデーション（空・500文字超）
     * 2. RDSに保存
     * 3. CloudWatchログ出力（Lambda連携用）
     */
    public void sendMessage(User sender, Long courseId, String content) {
        if (content == null || content.trim().isEmpty()) return;
        if (content.length() > 500) content = content.substring(0, 500);

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return;

        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setCourse(course);
        message.setContent(content.trim());
        message.setSentAt(LocalDateTime.now());
        chatMessageRepository.save(message);

        // CloudWatchログ出力（Lambda連携用）
        logger.info("[CHAT_EVENT] courseId={} sender={} role={} content={}",
                courseId, sender.getName(), sender.getRole(), content.trim());
    }

    /**
     * コースのメッセージ一覧取得（古い順）
     */
    public List<ChatMessage> getMessages(Long courseId) {
        return chatMessageRepository.findByCourse_IdOrderBySentAtAsc(courseId);
    }

    /**
     * 全メッセージ取得（管理者用・新しい順）
     */
    public List<ChatMessage> getAllMessages() {
        return chatMessageRepository.findAllByOrderBySentAtDesc();
    }

    /**
     * メッセージ削除（管理者のみ）
     * IDで1件削除する
     */
    public void deleteMessage(Long messageId) {
        chatMessageRepository.deleteById(messageId);
    }
}
