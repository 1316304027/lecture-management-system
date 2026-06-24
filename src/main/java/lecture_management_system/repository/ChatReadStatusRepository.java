package lecture_management_system.repository;

import lecture_management_system.entity.ChatReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChatReadStatusRepository extends JpaRepository<ChatReadStatus, Long> {

    Optional<ChatReadStatus> findByUser_IdAndCourse_Id(Long userId, Long courseId);
}
