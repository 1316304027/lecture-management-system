package lecture_management_system.service;

import lecture_management_system.dto.AvatarPresignResult;
import lecture_management_system.entity.User;
import lecture_management_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProfileService {

    private static final long MAX_AVATAR_BYTES = 2 * 1024 * 1024;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private S3Service s3Service;

    public AvatarPresignResult prepareAvatarUpload(User user, String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return null;
        }
        String lower = originalFileName.toLowerCase();
        if (!lower.endsWith(".jpg") && !lower.endsWith(".jpeg") && !lower.endsWith(".png")) {
            return null;
        }
        String ext = lower.endsWith(".png") ? ".png" : ".jpg";
        String s3Key = "avatars/" + user.getId() + "/" + UUID.randomUUID() + ext;
        String contentType = ext.equals(".png") ? "image/png" : "image/jpeg";
        String uploadUrl = s3Service.generatePresignedImageUploadUrl(s3Key, contentType);
        return new AvatarPresignResult(uploadUrl, s3Key);
    }

    public String saveAvatarKey(User user, String s3Key) {
        if (s3Key == null || !s3Key.startsWith("avatars/" + user.getId() + "/")) {
            return "invalid_key";
        }
        if (user.getAvatarS3Key() != null && !user.getAvatarS3Key().isBlank()) {
            s3Service.deleteFile(user.getAvatarS3Key());
        }
        user.setAvatarS3Key(s3Key);
        userRepository.save(user);
        return "success";
    }

    public String updateProfile(User user, String phone, String profileBio) {
        user.setPhone(phone != null ? phone.trim() : null);
        user.setProfileBio(profileBio != null ? profileBio.trim() : null);
        userRepository.save(user);
        return "success";
    }

    public String getAvatarUrl(User user) {
        if (user == null || user.getAvatarS3Key() == null || user.getAvatarS3Key().isBlank()) {
            return null;
        }
        return s3Service.generatePresignedUrl(user.getAvatarS3Key());
    }

    public Map<Long, String> buildAvatarUrlMap(List<User> users) {
        Map<Long, String> map = new HashMap<>();
        for (User u : users) {
            String url = getAvatarUrl(u);
            if (url != null) {
                map.put(u.getId(), url);
            }
        }
        return map;
    }

    public User refreshUser(User sessionUser) {
        if (sessionUser == null) {
            return null;
        }
        return userRepository.findById(sessionUser.getId()).orElse(sessionUser);
    }
}
