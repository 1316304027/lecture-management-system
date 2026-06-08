package lecture_management_system.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import java.io.IOException;
import java.time.Duration;

/**
 * S3操作の共通Service
 * ファイルのアップロード・署名付きURL生成・削除を担当する。
 *
 * EC2にIAMロール（AmazonS3FullAccess）を付与しているため
 * アクセスキー・シークレットキーのコーディングは不要。
 * EC2上では自動的にIAMロールの認証情報が使われる。
 */
@Service
public class S3Service {

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region:ap-northeast-1}")
    private String region;

    /**
     * S3にファイルをアップロードする
     * @param file        アップロードするファイル
     * @param s3Key       S3上のキー名（例: materials/12345_lecture.pdf）
     */
    public void uploadFile(MultipartFile file, String s3Key) throws IOException {
        S3Client s3 = buildS3Client();
        s3.putObject(
            PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType("application/pdf")
                .build(),
            RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );
        s3.close();
    }

    /**
     * S3の署名付きURL（Presigned URL）を生成する
     * 有効期限：10分
     * この URLをブラウザに返すと、ユーザーが直接S3からPDFをダウンロードできる。
     */
    public String generatePresignedUrl(String s3Key) {
        S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(region))
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(r -> r.bucket(bucketName).key(s3Key))
                .build();

        String url = presigner.presignGetObject(presignRequest).url().toString();
        presigner.close();
        return url;
    }

    /**
     * S3からファイルを削除する
     * @param s3Key 削除するファイルのS3キー名
     */
    public void deleteFile(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) return;
        S3Client s3 = buildS3Client();
        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build());
        s3.close();
    }

    private S3Client buildS3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
