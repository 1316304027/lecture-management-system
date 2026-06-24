package lecture_management_system.dto;

/** 管理者によるユーザー作成・パスワードリセット結果 */
public record PasswordOperationResult(
        String error,
        String email,
        boolean emailSent,
        String devSetupLink) {

    public static PasswordOperationResult fail(String error) {
        return new PasswordOperationResult(error, null, false, null);
    }

    public static PasswordOperationResult emailSent(String email) {
        return new PasswordOperationResult(null, email, true, null);
    }

    public static PasswordOperationResult emailFailed(String email, String devSetupLink) {
        return new PasswordOperationResult(
                "メール送信に失敗しました。SES設定を確認してください。",
                email, false, devSetupLink);
    }

    public boolean success() {
        return error == null;
    }
}
