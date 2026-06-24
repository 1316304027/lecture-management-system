# AWS SES パスワード設定 — 部署与发表说明

## 环境变量（EC2 Docker 必设）

```bash
export APP_BASE_URL=http://你的EC2公网IP或域名
export SES_FROM_EMAIL=你在SES验证过的发件邮箱
export MAIL_ENABLED=true
```

## AWS 控制台配置（体现 AWS 成果）

### 1. SES 验证发件邮箱
- AWS Console → SES → Verified identities → Create identity
- 验证 `SES_FROM_EMAIL`（如 noreply@yourdomain.com 或你的个人邮箱）

### 2. EC2 IAM 角色添加权限
```json
{
  "Effect": "Allow",
  "Action": ["ses:SendEmail", "ses:SendRawEmail"],
  "Resource": "*"
}
```

### 3. SES 沙盒模式
- 新账号默认 Sandbox：收件人邮箱也须在 SES 验证
- 发表 Demo 前：把 `teacher@test.com` 换成真实可收信邮箱，或在 SES 验证该地址
- 生产可申请 Production access

## 流程（发表 1 分钟日语台词）

1. 管理者创建用户（无密码框）→ Spring Boot 写 DB + 发 token
2. **AWS SES** 发邮件含 `/setup-password?token=xxx`
3. 用户点链接 → 自己设密码 → 才能登录
4. 管理者点 PWリセット → 同样 SES 再发链接（不知道密码）

## 本地开发

`application.yaml` 中设：
```yaml
app.mail.dev-show-link-on-failure: true
```
SES 失败时管理员画面会显示设置链接（仅开发用）。

## 测试账号

`admin@test.com` / `teacher@test.com` / `student@test.com` 密码 `Admin1234`
（PostConstruct 直接设定，不走 SES）
