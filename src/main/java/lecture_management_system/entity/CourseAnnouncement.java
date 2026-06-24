package lecture_management_system.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/** コースお知らせ（講師投稿 → 学生がコースポータルで閲覧） */
@Entity
@Table(name = "course_announcements")
@Data
public class CourseAnnouncement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
