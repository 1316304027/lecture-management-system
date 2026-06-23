package lecture_management_system.dto;

public record SubmissionCreatedRequest(
        Long studentId,
        Long assignmentId,
        String s3Key,
        String originalFileName
) {}
