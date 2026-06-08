package lecture_management_system.service;

import lecture_management_system.entity.*;
import lecture_management_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

/**
 * 教材業務Service
 * 【S3対応】ファイル保存先をローカルディスクからS3に変更。
 */
@Service
public class MaterialService {

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private S3Service s3Service;

    /** S3上のフォルダ名（プレフィックス） */
    private static final String S3_PREFIX = "materials/";

    public List<Material> findByCourseId(Long courseId) {
        return materialRepository.findByCourse_Id(courseId);
    }

    public List<Material> findPublishedByCourseId(Long courseId) {
        return materialRepository.findByCourse_IdAndPublishedTrue(courseId);
    }

    public Material findById(Long id) {
        return materialRepository.findById(id).orElse(null);
    }

    /**
     * 教材アップロード（講師SCR-102から使用）
     * 【S3対応】ファイルをS3にアップロードし、S3キーをDBに保存する。
     *
     * @return エラーメッセージ（null = 成功）
     */
    public String uploadMaterial(Long courseId, String title,
                                 boolean published, MultipartFile file,
                                 User createdBy) throws IOException {
        // バリデーション
        if (file == null || file.isEmpty()) return "ファイルを選択してください";
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            return "PDF形式のみアップロード可能です";
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            return "ファイルサイズ上限（10MB）を超えています";
        }
        if (title == null || title.trim().isEmpty()) return "教材タイトルを入力してください";
        if (title.length() > 100) return "100文字以内で入力してください";

        // S3にアップロード（キー名 = materials/タイムスタンプ_ファイル名）
        String storedFileName = System.currentTimeMillis() + "_" + originalName;
        String s3Key = S3_PREFIX + storedFileName;
        s3Service.uploadFile(file, s3Key);

        // DBにはS3キーを保存する
        Course course = courseRepository.findById(courseId).orElse(null);
        Material material = new Material();
        material.setCourse(course);
        material.setTitle(title.trim());
        material.setStoredFileName(s3Key);   // S3キーを保存
        material.setPublished(published);
        material.setCreatedBy(createdBy);
        materialRepository.save(material);
        return null;
    }

    public void togglePublished(Long materialId) {
        Material material = materialRepository.findById(materialId).orElse(null);
        if (material != null) {
            material.setPublished(!material.getPublished());
            materialRepository.save(material);
        }
    }

    /**
     * 教材削除
     * 【S3対応】S3からファイルも削除する。
     */
    public void deleteMaterial(Long materialId) {
        Material material = materialRepository.findById(materialId).orElse(null);
        if (material != null) {
            s3Service.deleteFile(material.getStoredFileName());
            materialRepository.deleteById(materialId);
        }
    }

    /**
     * ダウンロード用S3署名付きURLを取得する
     * 【S3対応】ローカルパスの代わりに署名付きURLを返す。
     * 有効期限：10分
     */
    public String getDownloadUrl(String storedFileName) {
        return s3Service.generatePresignedUrl(storedFileName);
    }
}
