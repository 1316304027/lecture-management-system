# AWS 上跑起来 — 步骤指南（复制粘贴版）

魏玉臻用 · 改完 UI 后在 EC2 上看效果

---

## 先懂一句话：什么叫「在 AWS 上跑」？

你的系统分 3 块，像一家店：

| 部分 | 是什么 | 在哪 |
|------|--------|------|
| **程序** | Spring Boot（网页、登录、课题） | **EC2** 服务器（一台云电脑） |
| **数据库** | PostgreSQL（用户、课程、出席） | **RDS**（云数据库） |
| **文件** | PDF 教材、作业 | **S3**（云硬盘） |

你在自己电脑上 **改代码** → 上传到 **GitHub** → **EC2 拉下来、重新打包、重启** → 用浏览器打开 EC2 的网址看效果。

**你不用在家连 RDS。** EC2 和 RDS 都在 AWS 里面，它们能互相说话。

---

## 你需要提前有的东西

- [ ] GitHub 账号，代码已 push 上去
- [ ] EC2 的 **公网 IP**（AWS 控制台 → EC2 → 实例 → 复制「公有 IPv4」）
- [ ] EC2 的 **SSH 密钥**（`.pem` 文件，研修时应该发过）
- [ ] 知道 EC2 登录用户名（一般是 `ec2-user`）

---

## 整体流程（每次改 UI 都重复这 5 步）

```
① 在 Cursor 里改代码（或让 AI 改）
② 在你电脑上 git commit + git push
③ SSH 登录 EC2
④ EC2 上 git pull + 打包 + 重启
⑤ 浏览器打开 http://<EC2的IP>/login 看效果
```

---

## 第 ① 步：在你电脑上提交代码

在 **WSL** 或 **PowerShell** 里：

```bash
cd /mnt/c/Users/yuzhe/lecture-management-system/demo
# PowerShell 用: cd C:\Users\yuzhe\lecture-management-system\demo

git status
git add .
git commit -m "ui: 第一版主题 login student-home"
git push
```

看到 `push` 成功就行。

---

## 第 ② 步：SSH 登录 EC2

**Windows PowerShell 示例**（把路径和 IP 换成你的）：

```powershell
ssh -i "C:\Users\yuzhe\你的密钥.pem" ec2-user@你的EC2公网IP
```

**WSL 示例：**

```bash
ssh -i /mnt/c/Users/yuzhe/你的密钥.pem ec2-user@你的EC2公网IP
```

第一次可能问 `Are you sure...` → 输入 `yes`。

登录成功后，提示符会变成类似：`[ec2-user@ip-xxx ~]$`

---

## 第 ③ 步：在 EC2 上更新并打包

根据你之前的环境，项目在 EC2 上多半是下面两个路径之一。**先试第一个**：

### 情况 A：项目在 `~/lecture-management-system`（最常见）

```bash
cd ~/lecture-management-system
git pull
./mvnw clean package -DskipTests
```

等最后出现 **`BUILD SUCCESS`**（可能要 3～5 分钟）。

### 情况 B：如果 A 报错「没有 git / 没有 mvnw」

```bash
cd ~/lecture-management-system/demo
git pull
./mvnw clean package -DskipTests
```

> 不确定路径？在 EC2 上执行：`find ~ -name "mvnw" 2>/dev/null`  
> 看输出在哪个文件夹，就 `cd` 到那个文件夹。

---

## 第 ④ 步：重启程序

### 方法 1：你用 Docker 跑（docker-compose.yml 那套）

在 **有 docker-compose.yml 的目录**（通常是 `~/lecture-management-system`）：

```bash
docker compose down
docker compose up -d
docker ps
```

`docker ps` 里应能看到 `app`、`nginx` 等容器在运行。

### 方法 2：你直接跑 JAR（以前对话里用过）

```bash
# 先停掉旧进程
pkill -f demo-0.0.1-SNAPSHOT.jar

# 后台启动新 JAR（路径按你实际 target 目录）
cd ~/lecture-management-system
nohup java -jar target/demo-0.0.1-SNAPSHOT.jar > app.log 2>&1 &

# 看日志有没有 Started
tail -f app.log
```

看到 `Started DemoApplication` 后按 `Ctrl+C` 退出 tail。

---

## 第 ⑤ 步：浏览器验证

打开：

```
http://你的EC2公网IP/login
```

如果配了 Nginx，用 **80 端口**（不用写 `:8080`）。

**检查清单：**

- [ ] 登录页是否是新的绿色渐变样式
- [ ] 能登录（用你 RDS 里已有的账号，不是 local 的 test.com）
- [ ] 学生主页是否是「受講者ダッシュボード」
- [ ] 出席 / 教材 / 課題 / チャット 能点进去

---

## 常见问题

### 1. 网页打不开 / 连接超时

- EC2 安全组 → **入站规则** 要有：**HTTP 80** 和/或 **8080**，来源 `0.0.0.0/0`
- 实例是否在「运行中」

### 2. git pull 要密码 / 失败

- 可能要在 EC2 上配 GitHub SSH key 或 Personal Access Token（问研修负责人）

### 3. BUILD SUCCESS 了但页面没变

- 浏览器 **强制刷新**：`Ctrl + Shift + R`
- 确认重启的是 **新 JAR**（docker compose down/up 或 pkill 后重启）
- 确认 `git pull` 真的拉到了最新 commit

### 4. 想还原 UI

在你电脑上：

```bash
git checkout -- src/main/resources/static/pcfa-theme.css
git checkout -- src/main/resources/templates/login.html
git checkout -- src/main/resources/templates/student-home.html
git commit -m "revert: ui v1"
git push
```

然后在 EC2 上再 **pull + 打包 + 重启** 一遍。

---

## 发表当天

1. 提前 30 分钟再跑一遍第 ③～⑤ 步  
2. 记下 Demo 用的 **账号密码**  
3. 手机拍几张截图备用（防网络问题）
