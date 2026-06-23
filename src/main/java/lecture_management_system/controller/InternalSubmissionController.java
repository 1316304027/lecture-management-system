package lecture_management_system.controller;

import lecture_management_system.dto.SubmissionCreatedRequest;
import lecture_management_system.service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/internal")
public class InternalSubmissionController {

    @Autowired private SubmissionService submissionService;

    @Value("${app.internal-token:LectureInternal2026!}")
    private String internalToken;

    @PostMapping("/submission-created")
    public ResponseEntity<Map<String, String>> submissionCreated(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @RequestBody SubmissionCreatedRequest body) {
        if (token == null || !token.equals(internalToken))
            return ResponseEntity.status(403).body(Map.of("error", "forbidden"));
        String result = submissionService.registerFromS3(
                body.studentId(), body.assignmentId(), body.s3Key(), body.originalFileName());
        return "success".equals(result)
                ? ResponseEntity.ok(Map.of("status", "ok"))
                : ResponseEntity.badRequest().body(Map.of("error", result));
    }
}