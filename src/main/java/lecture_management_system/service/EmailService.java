package lecture_management_system.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

/**
 * AWS SES によるパスワード設定メール送信。
 * EC2 の IAM ロールに ses:SendEmail 権限が必要。
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${aws.ses.from-email}")
    private String fromEmail;

    @Value("${aws.ses.region:ap-northeast-1}")
    private String region;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    /**
     * パスワード設定リンクをメール送信（新規作成・管理者リセット共通）
     * @return 送信成功なら true
     */
    public boolean sendPasswordSetupEmail(String toEmail, String userName, String token) {
        String setupLink = baseUrl + "/setup-password?token=" + token;
        String subject = "【PCFA受講管理】パスワード設定のお願い";
        String bodyText = """
                %s 様

                PCFA受講管理システムのアカウントが作成（またはパスワードリセット）されました。
                以下のリンクから、ご自身でパスワードを設定してください。

                %s

                ※ このリンクの有効期限は24時間です。
                ※ 管理者はあなたのパスワードを知ることはできません。

                PCFA受講管理システム
                """.formatted(userName, setupLink);

        String bodyHtml = """
                <p>%s 様</p>
                <p>PCFA受講管理システムのアカウントが作成（またはパスワードリセット）されました。<br>
                以下のボタンから、<strong>ご自身でパスワードを設定</strong>してください。</p>
                <p><a href="%s" style="display:inline-block;padding:12px 24px;background:#157347;color:#fff;text-decoration:none;border-radius:6px;">パスワードを設定する</a></p>
                <p style="font-size:12px;color:#666;">リンク: <a href="%s">%s</a></p>
                <p style="font-size:12px;color:#666;">※ 有効期限24時間。管理者はパスワードを知りません。</p>
                """.formatted(userName, setupLink, setupLink, setupLink);

        if (!mailEnabled) {
            log.warn("[SES無効] パスワード設定メール → {} : {}", toEmail, setupLink);
            return false;
        }

        try (SesClient ses = SesClient.builder().region(Region.of(region)).build()) {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(fromEmail)
                    .destination(Destination.builder().toAddresses(toEmail).build())
                    .message(Message.builder()
                            .subject(Content.builder().charset("UTF-8").data(subject).build())
                            .body(Body.builder()
                                    .text(Content.builder().charset("UTF-8").data(bodyText).build())
                                    .html(Content.builder().charset("UTF-8").data(bodyHtml).build())
                                    .build())
                            .build())
                    .build();
            ses.sendEmail(request);
            log.info("SES: パスワード設定メール送信成功 → {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("SES: メール送信失敗 → {} : {}", toEmail, e.getMessage());
            return false;
        }
    }

    public String buildSetupLink(String token) {
        return baseUrl + "/setup-password?token=" + token;
    }
}
