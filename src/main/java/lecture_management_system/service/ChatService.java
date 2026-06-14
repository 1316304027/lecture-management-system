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
 * チャットメッセージの送信・取得を担当する。
 *
 * 【Lambda連携の仕組み】
 * メッセージを送信するたびにログを出力する。
 * このログはdocker-composeの「awslogs」設定でCloudWatchに送られる。
 * CloudWatchのログをLambdaのトリガーにすることで、
 * 「メッセージが送信された」イベントを検知してLambdaを起動できる。
 *
 * 【RDS連携】
 * メッセージはRDSのchat_messagesテーブルに保存される。
 * サーバーを再起動してもメッセージが消えない。
 */
@Service
public class ChatService {

    // ログ出力用（CloudWatch連携のために使用）
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private CourseRepository courseRepository;

    /**
     * メッセージを送信する（RDSに保存）
     *
     * 処理の流れ：
     * 1. Courseエンティティを取得
     * 2. ChatMessageエンティティを作成
     * 3. 送信日時を現在時刻にセット
     * 4. RDS（PostgreSQL）に保存
     * 5. CloudWatchログに記録（Lambda連携用）
     *
     * @param sender   送信者（ログイン中のUser）
     * @param courseId 対象コースID
     * @param content  メッセージ本文
     */
    public void sendMessage(User sender, Long courseId, String content) {
        // バリデーション：空メッセージは保存しない
        if (content == null || content.trim().isEmpty()) return;
        if (content.length() > 500) content = content.substring(0, 500);

        // コースを取得
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return;

        // メッセージエンティティを作成してRDSに保存
        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setCourse(course);
        message.setContent(content.trim());
        message.setSentAt(LocalDateTime.now());
        chatMessageRepository.save(message);

        // CloudWatchログに記録（Lambda連携用）
        // このログがCloudWatchに送られ、Lambdaのトリガーになる
        logger.info("[CHAT_EVENT] courseId={} sender={} role={} content={}",
                courseId, sender.getName(), sender.getRole(), content.trim());
    }

    /**
     * コースのチャット履歴を取得する（古い順）
     *
     * @param courseId 対象コースID
     * @return メッセージリスト（古い順）
     */
    public List<ChatMessage> getMessages(Long courseId) {
        return chatMessageRepository.findByCourse_IdOrderBySentAtAsc(courseId);
    }

    /**
     * 全コースのメッセージを取得する（管理者用・新しい順）
     *
     * @return 全メッセージリスト
     */
    public List<ChatMessage> getAllMessages() {
        return chatMessageRepository.findAllByOrderBySentAtDesc();
    }
}
