# 前端说明

## 开发

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build
```

## API代理配置

在 `vite.config.js` 中配置代理：

```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

## 功能页面

| 页面 | 路由 | 权限 | 说明 |
|------|------|------|------|
| 登录 | /login | 公开 | 用户登录 |
| 首页 | /dashboard | 全部 | 数据仪表盘 |
| 用户管理 | /user | 管理员 | 用户CRUD |
| 班级管理 | /class | 管理员 | 班级CRUD |
| 报告管理 | /report | 全部 | 报告上传与查看 |
| 评价管理 | /evaluation | 全部 | AI/手动评价 |
| 统计报表 | /statistics | 全部 | 成绩统计分析 |
| 系统设置 | /config | 管理员 | 系统配置 |
