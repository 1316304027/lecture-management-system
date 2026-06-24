# 第四版 UX 修正 — 详细理解报告（6/29 发表必背）

魏玉臻 · PCFA 受講管理システム

**网页阅读 / 导出 PDF：** 请打开同目录下的 `第四版_UX修正_理解报告.html`，浏览器中 **Ctrl+P → 另存为 PDF**。

---

## 一、核心原则（最重要）

| 画面 | 职责 |
|------|------|
| 実績レポート | 看数字：出席率、提出率、分数 |
| ユーザー管理 | 管人：账号 + 详细プロフィール |
| 教材 / 課題 | 管内容：是否对**受讲者显示** |
| お知らせ | 讲师发通知，学生在コースポータル看 |

**日语必背：**

> 実績レポートは成績の一覧用です。受講者の詳細プロフィールは、ユーザー管理に集約しました。

---

## 二、修改 1：「公开课」误解

「公開」= 受讲者能否看到教材/课题（`published` 字段），不是对外公开课。

**日语：** 「公開」は受講者への表示設定であり、外部公開のコースではありません。

---

## 三、修改 2：プロフィール只从ユーザー管理进

- 实绩报告：头像 + 姓名，**无**プロフィール按钮
- 用户管理：完整详情 + S3 头像

**链路：** `GET /admin/reports` → `ReportService` + `ProfileService.buildAvatarUrlMap`  
**详情：** `GET /admin/accounts/{id}/profile` → `profile-card.html`

---

## 四、修改 3：お知らせ独立页面

- 讲师：`GET /instructor/announcements?courseId=`
- 学生：`GET /student/course?courseId=` 只读
- 表：`course_announcements`

**日语：** お知らせ機能を教材管理から分離し、専用画面で投稿します。

---

## 五、修改 4：日期下拉框

`fragments/date-picker.html` → `AdminController.parseDate(year, month, day)`

**日语：** 授業日は年・月・日のドロップダウンで統一しました。

---

## 六、Demo 路线 + Q&A

见 HTML 完整版（含 10 道 Q&A 日语参考答案）。

完整路径：`docs/第四版_UX修正_理解报告.html`
