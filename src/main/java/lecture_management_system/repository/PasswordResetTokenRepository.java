package lecture_management_system.repository;

import lecture_management_system.entity.PasswordResetToken;
import lecture_management_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);

    List<PasswordResetToken> findByUserAndUsedFalse(User user);
}
