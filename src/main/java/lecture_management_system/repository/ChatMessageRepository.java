package lecture_management_system.repository;

import lecture_management_system.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * チャットメッセージRepository
 *
 * 【機能説明】
 * JpaRepositoryを継承するだけで、基本的なDB操作（保存・取得・削除）が
 * 自動で使えるようになる。Spring Bootの仕組み。
 *
 * 【メソッド説明】
 * findByCourse_IdOrderBySentAtAsc:
 *   「course_id = ○○」のメッセージを「sentAt（送信日時）の昇順」で取得
 *   = コースのチャット履歴を古い順に取得する
 *
 * findAllByOrderBySentAtDesc:
 *   全メッセージを送信日時の降順（新しい順）で取得
 *   = 管理者が全チャットを確認するために使用
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * コースIDで絞り込んで古い順に取得（チャット画面用）
     */
    List<ChatMessage> findByCourse_IdOrderBySentAtAsc(Long courseId);

    /**
     * 全メッセージを新しい順で取得（管理者用）
     */
    List<ChatMessage> findAllByOrderBySentAtDesc();
}
