# 课序 · 大学生课表系统

基于 Vue 3、Spring Boot 和 MySQL 的课表系统。支持注册及用户/管理员登录、个人课程增删改查、CSV 导入、周课表、今日课程、课程统计、页面打开时的浏览器提醒，以及 OpenAI 兼容接口的课程问答。界面参考指定网站的配色与排版；标志使用参考网站前端内嵌的 SVG。

## 快速开始

准备 Java 17、Maven、Node.js 20+ 和 MySQL 8。创建数据库和独立用户后，在 `backend` 设置环境变量 `DB_URL`、`DB_USER`、`DB_PASSWORD`，执行 `mvn spring-boot:run`。在 `frontend` 执行 `npm install` 和 `npm run dev`。开发服务器通过 Vite 代理 `/api` 至 8080 端口。详细步骤及腾讯云部署见 [部署指南](部署指南.md)。

## 导入

页面提供 CSV 模板。列为 `课程名称,教师,地点,星期,开始节,结束节,开始周,结束周`，星期为 1–7；编码使用 UTF-8。导入按行保存，出错时保留已成功的记录并显示行号。教务系统没有统一协议，当前没有学校教务系统直连；可将教务导出数据转换为模板导入，未来可在服务端增加具体学校适配器。

## 设计及限制

- 用户使用服务端 Session 登录；课程以账户 ID 隔离。管理员账号通过启动环境变量首次创建。管理员可查看用户列表。
- 浏览器提醒在授权且打开页面时提示当天课程，不提供定时后台推送。周次由用户手动切换。
- 课程助手可设置兼容 Chat Completions 的 API 地址、密钥、模型、角色与随机性。用户设置按账号保存在当前浏览器的 localStorage（含明文密钥，共享设备请清除），不会跨设备同步；三项 API 设置留空时使用服务端默认配置。请求由服务器转发，允许的 HTTPS 域名由 `AI_ALLOWED_HOSTS` 配置，每次提问为独立请求，不发送课表。
- 数据表通过 Hibernate `ddl-auto=update` 创建；生产迭代应引入 Flyway 等迁移工具，做好备份和回滚。
- API 在 `/api` 下；课程接口 `GET/POST /courses`、`PUT/DELETE /courses/{id}`，认证接口 `/auth/register`、`/auth/login`、`/auth/logout`、`/auth/me`，管理员接口 `/admin/users`，模型接口 `/ai/chat`。

## 开源

采用 MIT 许可证。参考站名称及视觉元素的权利属于其相应权利人，发布前请确认使用授权。
