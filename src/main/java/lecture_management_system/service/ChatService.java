package lecture_management_system.service;

import lecture_management_system.entity.*;
import lecture_management_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    @Autowired private ChatMessageRepository chatMessageRepository;
    @Autowired private ChatReadStatusRepository chatReadStatusRepository;
    @Autowired private CourseRepository courseRepository;

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

        logger.info("[CHAT_EVENT] courseId={} sender={} role={} content={}",
                courseId, sender.getName(), sender.getRole(), content.trim());
    }

    public List<ChatMessage> getMessages(Long courseId) {
        return chatMessageRepository.findByCourse_IdOrderBySentAtAsc(courseId);
    }

    public List<ChatMessage> getAllMessages() {
        return chatMessageRepository.findAllByOrderBySentAtDesc();
    }

    public void deleteMessage(Long messageId) {
        chatMessageRepository.deleteById(messageId);
    }

    /** チャット画面を開いたら既読にする */
    public void markCourseAsRead(User user, Long courseId) {
        if (user == null || courseId == null) return;
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return;
        ChatReadStatus status = chatReadStatusRepository
                .findByUser_IdAndCourse_Id(user.getId(), courseId)
                .orElse(new ChatReadStatus());
        if (status.getId() == null) {
            status.setUser(user);
            status.setCourse(course);
        }
        status.setLastReadAt(LocalDateTime.now());
        chatReadStatusRepository.save(status);
    }

    /** コースごとの未読件数（自分以外が送ったメッセージ） */
    public long countUnread(User user, Long courseId) {
        if (user == null) return 0;
        return chatReadStatusRepository.findByUser_IdAndCourse_Id(user.getId(), courseId)
                .map(s -> chatMessageRepository.countByCourse_IdAndSentAtAfterAndSender_IdNot(
                        courseId, s.getLastReadAt(), user.getId()))
                .orElseGet(() -> chatMessageRepository.countByCourse_IdAndSender_IdNot(
                        courseId, user.getId()));
    }

    /** ホーム画面用：複数コースの未読マップ */
    public Map<Long, Long> buildUnreadMap(User user, List<Course> courses) {
        Map<Long, Long> map = new HashMap<>();
        if (user == null || courses == null) return map;
        for (Course c : courses) {
            long n = countUnread(user, c.getId());
            if (n > 0) map.put(c.getId(), n);
        }
        return map;
    }

    public long countUnreadTotal(User user, List<Course> courses) {
        return buildUnreadMap(user, courses).values().stream().mapToLong(Long::longValue).sum();
    }
}
