# 后端说明

## 配置文件

在 `src/main/resources/application.yml` 中配置数据库连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/training_system
    username: root
    password: your_password
```

## 打包部署

```bash
mvn clean package
java -jar target/training-system-1.0.0.jar
```

## API测试

可以使用 curl 测试接口：

```bash
# 登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 获取用户信息（需携带Token）
curl http://localhost:8080/api/auth/userinfo \
  -H "Authorization: Bearer your_token"
```
