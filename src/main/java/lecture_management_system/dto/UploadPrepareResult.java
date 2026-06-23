package lecture_management_system.dto;

public record UploadPrepareResult(String errorCode, PresignUploadResponse response) {
    public static UploadPrepareResult ok(PresignUploadResponse r) { return new UploadPrepareResult(null, r); }
    public static UploadPrepareResult err(String e) { return new UploadPrepareResult(e, null); }
    public boolean isOk() { return errorCode == null; }
}
