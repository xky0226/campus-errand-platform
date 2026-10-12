# springai_exam —— 作业二：校园百事通（Spring AI 领域智能助手）

## 一、技术栈
- JDK 17（本机 17.0.19）
- Spring Boot 4.1.0
- Spring AI 2.0.0（spring-ai-bom 统一管理）
- Maven 3.9.14
- 端口 8080

## 二、目录结构
```
springai_exam/
├── pom.xml
├── src/main/java/com/example/exam/
│   ├── SpringaiExamApplication.java
│   ├── config/
│   │   ├── AiConfig.java               # 挂载三个 Advisor
│   │   ├── RewritePromptAdvisor.java   # 自定义 BaseAdvisor：改写用户问题
│   │   └── CorsConfig.java             # 全局跨域
│   └── controller/
│       └── ExamController.java         # /exam/chat、/exam/stream、/exam/template
├── src/main/resources/
│   ├── application.yaml
│   └── prompt/exam_prompt.txt          # 外部化的人设（校园百事通）
├── tools/                              # 自测与出图脚本（非交付代码，仅个人自测用）
├── evidence/                           # 真实接口返回的原始证据
├── shots/                              # 渲染出来的插图
└── logs/                               # 启动日志
```

## 三、启动前准备
环境变量里要有 `DEEPSEEK_API_KEY`（当前 application.yaml 读的是它）。
换成阿里云百炼：把 api-key 改成 `${DASHSCOPE_API_KEY:sk-xxx}`，
base-url 改成 `https://dashscope.aliyuncs.com/compatible-mode/v1`，model 改成百炼的模型名。

## 四、启动命令
```bash
mvn spring-boot:run
# 或者
mvn package -DskipTests
java -jar target/springai_exam-0.0.1-SNAPSHOT.jar
```
> 建议在 VM options 里加 `-Dfile.encoding=UTF-8`，否则控制台日志里的中文会乱码
> （Windows 默认代码页是 GBK）。实测启动命令：
> `java -jar target/springai_exam-0.0.1-SNAPSHOT.jar --logging.charset.console=UTF-8`

## 五、接口清单
| 任务 | 方法 | URL | 说明 |
|------|------|-----|------|
| 任务二 | GET | `/exam/chat?question=你好` | 同步对话，参数默认值「你是谁」，用 `call()` |
| 任务三 | GET | `/exam/stream?question=图书馆几点开门` | 流式对话，参数默认值「你能做什么」，返回 `Flux<String>` |
| 任务四 | GET | `/exam/template?topic=食堂&question=中午吃什么` | `PromptTemplate` 双占位符，用 `call()` |

## 六、Advisor 拦截链
`AiConfig` 里用 `.defaultAdvisors(...)` 挂三个，顺序由 `getOrder()` 决定，数值越小越先执行：

| Advisor | order | 作用 |
|---------|-------|------|
| `SimpleLoggerAdvisor` | 0（默认） | 打印请求 / 响应，需配 `logging.level.org.springframework.ai: debug` |
| `SafeGuardAdvisor` | 1 | 敏感词拦截（代写 / 刷单 / 赌博 / 色情 / 暴力 / 诈骗 / 毒品），命中直接返回兜底语，不请求模型 |
| `RewritePromptAdvisor` | 2 | 自定义：用 `PromptTemplate` 把用户问题改写成「先给结论、再分点解释」的框架 |

## 七、几个实测踩到的坑
1. 作业文档写的依赖 `spring-ai-starter-model-webmvc` 在 Maven 中央仓库不存在，实际用 `spring-boot-starter-webmvc`。
2. 讲义里 `BaseAdvisor` 的 import 包名不对，实际在 `org.springframework.ai.chat.client.advisor.api` 下。
3. `.system(Resource)` 单参重载内部用 `Charset.defaultCharset()`，中文 Windows 上是 GBK，
   会把 UTF-8 的 txt 读成乱码；本作业改为 `.system(examPrompt, StandardCharsets.UTF_8)`。
4. 讲义示例的 `before()` 把整个 Prompt 换掉会丢掉 System 消息，导致人设失效；
   本作业只重写用户消息，用 `prompt.mutate().messages(...)` 保留 System 消息。
5. 控制台日志重定向到文件时中文乱码，加 `--logging.charset.console=UTF-8` 解决。
