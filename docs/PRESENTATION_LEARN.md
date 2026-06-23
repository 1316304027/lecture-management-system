# 发表学习笔记 — 第一版（login + student-home）

魏玉臻 · 边改边学 · 6/29 総まとめ報告会用

---

## 第一版改了什么？

| 文件 | 作用 |
|------|------|
| `static/pcfa-theme.css` | 全站统一颜色、卡片、登录页、导航栏样式 |
| `templates/login.html` | 登录页视觉升级（渐变背景 + 专业卡片） |
| `templates/student-home.html` | 学生主页 Dashboard 风（统计条 + 课程卡片） |

**没有改 Java 代码** → 原有登录、权限、课程列表逻辑不变，系统不会因为这个版本崩掉。

---

## 本地怎么看？

**重要：** 默认 `application.yaml` 连的是 **AWS RDS**，家里网络通常连不上（会 timeout）。
本地看 UI 请用 **local 配置 + 本机数据库**：

```bash
cd /mnt/c/Users/yuzhe/lecture-management-system/demo

# 1. 启动本机 PostgreSQL（只需第一次，或数据库停了时再跑）
docker compose -f docker-compose.local.yml up -d

# 2. 用 local 配置启动（连 localhost，不连 AWS）
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Windows PowerShell 第 2 步：

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

浏览器：`http://localhost:8080/login`

测试账号（启动时自动创建）：

| 角色 | 邮箱 | 密码 |
|------|------|------|
| 学生 | student@test.com | Admin1234 |
| 讲师 | teacher@test.com | Admin1234 |
| 管理员 | admin@test.com | Admin1234 |

**发表 Demo 用线上版：** 改完代码 push 到 GitHub → EC2 上 pull 重部署 → 浏览器打开 EC2 的 IP。

---

## 发表用：登录页（30秒日语）

> こちらが PCFA 受講管理システムのログイン画面です。  
> メールとパスワードで認証し、役割（管理者・講師・学生）に応じて画面が切り替わります。

### 可能被问

**Q: ログインはどう実装していますか？**

> `LoginController` が POST `/login` を受け取り、メールとパスワードでユーザーを検証します。  
> 成功すると `HttpSession` にユーザー情報を保存し、ロールに応じてリダイレクトします。

**代码位置：** `controller/LoginController.java`

---

## 发表用：学生主页（45秒日语）

> 学生がログインすると、このダッシュボードが表示されます。  
> 受講中のコースごとに、出席登録・教材・課題提出・チャットへ遷移できます。

### 请求怎么走？（你要能画这条线）

```
浏览器 GET /student/home
    → StudentController.home()
    → courseService.getStudentCourses(学生ID)
    → Thymeleaf 渲染 student-home.html
    → 画面显示课程卡片
```

**三层对应：**

- **Controller** = `StudentController`（收请求、取 Session 用户）
- **Service** = `CourseService`（查这个学生选了哪些课）
- **画面** = `student-home.html`（Thymeleaf 模板，不是 React）

### 可能被问

**Q: なぜ画面を改善しましたか？**

> ご指摘を受け、受講者が最初に見る画面をダッシュボード形式にし、  
> コース単位で機能へ迷わず遷移できるようにしました。

---

## 第一版之后（第二版预告）

- 出席 / 课题 / 聊天 页面套同一主题
- 管理员课程管理页抛光
- 每页追加一节本笔记 + Q&A

---

## 自检（改完后你做这 3 件事）

- [ ] 本地能打开 login，能登录
- [ ] 学生账号能看到新主页，四个按钮都能点进去
- [ ] 用自己的话说出：`/student/home` 经过哪个 Controller
