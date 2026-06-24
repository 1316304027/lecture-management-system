# 发表学习笔记 — 全版本（边改边学 · 6/29 用）

魏玉臻 · PCFA 受講管理システム

---

## 版本一览

| 版本 | 内容 | 状态 |
|------|------|------|
| **第一版** | login + student-home + pcfa-theme.css | ✅ |
| **第二版** | 出席/课题/聊天 + admin-courses + 実績レポート + プロフィール | ✅ |
| **第三版** | Dashboard 统计 + コースポータル + お知らせ | ✅ |
| **第四版** | UX修正：プロフィール集約・お知らせ分離・日付UI・管理者画面統一 | ✅ |

> **第四版详细理解报告（必读）：** `docs/第四版_UX修正_理解报告.md`

---

# 第一版：登录 + 学生主页

## 改了什么？

| 文件 | 作用 |
|------|------|
| `static/pcfa-theme.css` | 全站统一颜色、卡片、导航栏 |
| `templates/login.html` | 渐变背景 + 专业登录卡 |
| `templates/student-home.html` | Dashboard 风课程卡片 |

## 本地怎么看？

```bash
docker compose -f docker-compose.local.yml up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

浏览器：`http://localhost:8080/login`

| 角色 | 邮箱 | 密码 |
|------|------|------|
| 学生 | student@test.com | Admin1234 |
| 讲师 | teacher@test.com | Admin1234 |
| 管理员 | admin@test.com | Admin1234 |

## 发表一句话（登录）

> こちらがログイン画面です。メールとパスワードで認証し、役割に応じて画面が切り替わります。

**Q: ログインの仕組み？** → `LoginController` + `HttpSession` + BCrypt

## 发表一句话（学生主页）

> 受講者ダッシュボードで、コースごとに出席・教材・課題・チャットへ遷移できます。

**链路：** `GET /student/home` → `StudentController` → `CourseService` → `student-home.html`

---

# 第二版：核心业务 Demo 页

## 出席登録 `/student/attendance?courseId=`

> 授業日に合わせて出席登録ができ、出席率が自動計算されます。

**链路：** `StudentController.attendancePage()` → `AttendanceService` → RDS `attendances`

## 課題提出 `/student/assignments?courseId=`

> 課題PDFは Presigned URL で S3 に直接アップロードします。

**链路：** presign → 浏览器 PUT S3 → POST submit → `submissions` 表

## チャット `/chat?courseId=`

> コース単位のチャットです。未読件数は DB の既読管理でホームに表示します。

**链路：** `ChatController` → `ChatService` → `chat_messages` + `chat_read_status`

## 管理者 コース管理 `/admin/courses`

> コース・講師・学生・授業日を一元管理します。時刻はドロップダウンで入力します。

## 実績レポート `/admin/reports`

> 出席率・提出率・採点状況をコース別に一覧します。未採点は0点扱いせず「採点待ち」表示です。

## プロフィール（S3 头像）

> 受講者の顔写真は S3 に保存し、講師と管理者がプロフィールから確認できます。

---

# 第三版：Dashboard + コースポータル + お知らせ

## Dashboard 统计（学生ホーム顶部）

| 统计 | 数据来源 |
|------|----------|
| 平均出席率 | `AttendanceService.calculateRate()` |
| 未提出課題 | 公开课题数 − 已提交数 |
| 未読チャット | `ChatService.buildUnreadMap()` |

**代码：** `StudentDashboardService.java`

## コースポータル `/student/course?courseId=`

> コースごとのポータル画面で、お知らせ・統計・機能へのショートカットを一覧できます。

**链路：** `StudentController.coursePortal()` → 公告 + 教材 + 课题列表

## お知らせ（课程公告）

> 講師は**専用のお知らせ画面**から投稿し、学生がコースポータルで閲覧します（教材管理とは分離）。

**表：** `course_announcements`  
**讲师发帖：** `GET/POST /instructor/announcements`  
**学生查看：** コースポータル

### 发表一句话

> お知らせ機能を教材画面から分離し、講師は専用画面で投稿、受講者はコースポータルで閲覧します。

## 第四版 UX 修正（プロフィール・公開文言・日付UI）

> 実績レポートは成績一覧、プロフィール詳細はユーザー管理に集約しました。「公開」は受講者への表示設定です。

**详细讲解 + 日语 Q&A：** 见 `docs/第四版_UX修正_理解报告.md`

---

# 每层你要记住什么（面试用）

```
Controller = 接待员（收 HTTP、检查 Session）
Service    = 业务员（算逻辑、调多个 Repository）
Repository = 仓库管理员（数据库 CRUD）
Entity     = 表结构
templates  = 画面（Thymeleaf）
```

---

# AWS 发表 3 分钟台词

> 本システムは EC2 上の Docker で Spring Boot を稼働させ、  
> データは RDS PostgreSQL、ファイルは S3 に保存しています。  
> 大容量ファイルは Presigned URL でブラウザから S3 へ直接アップロードし、  
> サーバー負荷を抑えています。

架构图见：`docs/AWS架构图.md`  
完整稿见：`docs/发表稿_15分钟.md`

---

# 自检清单

- [ ] 本地或 EC2 能登录三种角色
- [ ] 学生ホーム有 4 个统计数字
- [ ] コース詳細 → お知らせ能看到
- [ ] 讲师发帖 → 学生刷新能看到
- [ ] チャット未読红点 → 打开后消失
- [ ] 実績レポート未採分显示「採点待ち」
- [ ] 能说出 `/student/home` 经过哪个 Controller

---

# 被问「AI 写的吗？」

> Cursor を補助ツールとして使いましたが、要件整理、動作確認、AWS デプロイ、不具合修正は自分で行いました。設計の意図は説明できます。
