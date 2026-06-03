package lecture_management_system.service;

import lecture_management_system.entity.*;
import lecture_management_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

/**
 * 教材業務Service
 * 教材のアップロード・一覧取得・ダウンロード・公開管理を担当する。
 */
@Service
public class MaterialService {

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private CourseRepository courseRepository;

    /** ファイル保存ディレクトリ（application.yamlで設定） */
    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    /** コース別教材一覧取得（講師用：公開・非公開全件） */
    public List<Material> findByCourseId(Long courseId) {
        return materialRepository.findByCourse_Id(courseId);
    }

    /** コース別公開教材一覧取得（受講者用） */
    public List<Material> findPublishedByCourseId(Long courseId) {
        return materialRepository.findByCourse_IdAndPublishedTrue(courseId);
    }

    /** 教材詳細取得 */
    public Material findById(Long id) {
        return materialRepository.findById(id).orElse(null);
    }

    /**
     * 教材アップロード（講師SCR-102から使用）
     *
     * 【バリデーション】
     * - PDF形式のみ
     * - 10MB以内
     *
     * @return エラーメッセージ（null = 成功）
     */
    public String uploadMaterial(Long courseId, String title,
                                 boolean published, MultipartFile file,
                                 User createdBy) throws IOException {
        // バリデーション
        if (file.isEmpty()) return "ファイルを選択してください";
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            return "PDF形式のみアップロード可能です";
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            return "ファイルサイズ上限（10MB）を超えています";
        }
        if (title == null || title.trim().isEmpty()) return "教材タイトルを入力してください";
        if (title.length() > 100) return "100文字以内で入力してください";

        // ファイル保存（タイムスタンプ+元ファイル名で一意なファイル名生成）
        Files.createDirectories(Paths.get(uploadDir));
        String storedFileName = System.currentTimeMillis() + "_" + originalName;
        Files.copy(file.getInputStream(),
                Paths.get(uploadDir, storedFileName),
                StandardCopyOption.REPLACE_EXISTING);

        // DB登録
        Course course = courseRepository.findById(courseId).orElse(null);
        Material material = new Material();
        material.setCourse(course);
        material.setTitle(title.trim());
        material.setStoredFileName(storedFileName);
        material.setPublished(published);
        material.setCreatedBy(createdBy);
        materialRepository.save(material);
        return null;
    }

    /**
     * 公開状態の切り替え（講師SCR-102から使用）
     * published=true にすれば受講者に表示される。
     */
    public void togglePublished(Long materialId) {
        Material material = materialRepository.findById(materialId).orElse(null);
        if (material != null) {
            material.setPublished(!material.getPublished());
            materialRepository.save(material);
        }
    }

    /** 教材削除（講師SCR-102から使用） */
    public void deleteMaterial(Long materialId) {
        materialRepository.deleteById(materialId);
    }

    /**
     * ファイルパス取得（ダウンロード時にControllerで使用）
     */
    public Path getFilePath(String storedFileName) {
        return Paths.get(uploadDir, storedFileName);
    }
}