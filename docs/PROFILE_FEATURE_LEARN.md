# S3 プロフィール機能 — 学习笔记（魏玉臻用）

## 这次加了什么？

| 功能 | URL | 谁用 |
|------|-----|------|
| プロフィール編集 | `/student/profile` | 学生上传头像、电话、自我介绍 |
| プロフィール预览 | `/student/profile/view` | 学生看自己 |
| 讲师看学生 | `/instructor/students/{id}/profile?courseId=` | 讲师 |
| 管理员看学生 | `/admin/accounts/{id}/profile` | 管理员 |

## 数据存在哪？

| 数据 | 存在哪 |
|------|--------|
| 电话、自我介绍、头像路径 | **RDS** `users` 表新列：`phone`, `profile_bio`, `avatar_s3_key` |
| 头像图片文件 | **S3** 路径如 `avatars/3/uuid.jpg` |

## 上传流程（和课题 PDF 一样）

```
1. 学生选图片
2. 浏览器 GET /student/profile/presign-avatar → 拿到 uploadUrl + s3Key
3. 浏览器 PUT 图片到 S3（Presigned URL）
4. 学生点「保存」POST /student/profile → DB 保存 s3Key + 电话 + 简介
5. 显示时 ProfileService.getAvatarUrl() → S3 Presigned GET URL（10分钟有效）
```

## 代码文件（你要知道的）

| 文件 | 作用 |
|------|------|
| `entity/User.java` | 加了 3 个字段 |
| `service/ProfileService.java` | 头像上传准备、保存、取 URL |
| `service/S3Service.java` | `generatePresignedImageUploadUrl` |
| `controller/StudentController.java` | 学生 profile 页面 |
| `controller/InstructorController.java` | 讲师看 profile |
| `controller/AdminController.java` | 管理员看 profile |
| `templates/student-profile.html` | 编辑页 |
| `static/pcfa-theme.css` | 头像圆形样式 |

## 发表 Demo 路线（加 1 分钟）

1. 学生登录 → ホーム → **プロフィール編集**
2. 上传头像 + 写自我介绍 → 保存
3. **プレビュー** 确认
4. 讲师登录 → 出欠確認 → 点某学生 **プロフィール** → 能看到头像
5. 管理员 → ユーザー管理 → 受講者 **プロフィール**

## 日语台词（30秒）

> 受講者のプロフィールと顔写真を AWS S3 に保存し、  
> Presigned URL でブラウザから直接アップロードします。  
> 講師と管理者はコース画面から受講者のプロフィールを確認できます。

## 部署（EC2 Docker）

```bash
git add .
git commit -m "feat: student profile S3 avatar + UI"
git push

# EC2:
cd ~/lecture-management-system
git pull
./mvnw clean package -DskipTests
sudo docker restart app-app-1
```

## 可能问

**Q: なぜ Cognito ではなく S3 ですか？**  
> 研修期間中は既存ログインを維持し、ファイル保存は S3 の Presigned URL で実装しました。認証統合は今後の課題です。
