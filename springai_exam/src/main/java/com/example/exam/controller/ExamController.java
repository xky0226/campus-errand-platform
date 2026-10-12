package com.example.exam.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 「校园百事通」助手对外接口。
 * 统一用 @RequiredArgsConstructor + final 做构造器注入。
 *
 * 任务二：GET /exam/chat
 * 任务三：GET /exam/stream
 * 任务四：GET /exam/template
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/exam")
public class ExamController {

    private final ChatClient chatClient;

    /**
     * 任务四-1：人设不再写在 Java 里，而是抽到 resources/prompt/exam_prompt.txt。
     * ChatClient 的 .system() 支持直接传 Resource，Spring AI 会自动读文件内容。
     * 以后改人设只要改这个 txt，Java 代码一行都不用动。
     */
    @Value("classpath:prompt/exam_prompt.txt")
    private Resource examPrompt;

    /**
     * 任务二：同步对话。等模型把回答全部生成后一次性返回。
     * 访问：/exam/chat?question=你好
     * 不传参数时按默认问题「你是谁」回答。
     */
    @GetMapping("/chat")
    public String chat(@RequestParam(value = "question", defaultValue = "你是谁") String question) {
        return chatClient
                .prompt()        // 创建对话请求
                .user(question)  // 设置用户问题
                .call()          // 同步调用，等待完整结果
                .content();      // 取出文本
    }

    /**
     * 任务三：流式对话 + 角色预设。
     * 返回 Flux&lt;String&gt;，文字逐块出现；人设与规则由 exam_prompt.txt 提供（任务四-1）。
     * 访问：/exam/stream?question=图书馆几点开门
     */
    @GetMapping("/stream")
    public Flux<String> stream(@RequestParam(value = "question", defaultValue = "你能做什么") String question) {
        return chatClient
                .prompt()
                // 设定角色与规则（来自外部 txt）。
                // 这里必须显式指定 UTF-8：.system(Resource) 的单参重载内部走的是
                // Charset.defaultCharset()，而中文 Windows 上 JDK 17 的默认字符集是 GBK，
                // 会把 UTF-8 的 txt 读成乱码（讲义 5-3 常见问题 Q1 提到过这个坑）。
                // 讲义原写法是 .system(examPrompt)，本机实测会乱码，故补上字符集参数。
                .system(examPrompt, StandardCharsets.UTF_8)
                .user(question)
                .stream()            // 流式调用
                .content();          // 返回 Flux<String>
    }

    /**
     * 任务四-2：PromptTemplate 动态拼接。
     * 模板里有两个占位符 {topic} 和 {question}，用 Map 传值完成替换。
     * 访问：/exam/template?topic=食堂&question=中午吃什么
     */
    private static final String TOPIC_TEMPLATE = """
            请围绕「{topic}」这一主题，回答用户的问题：{question}
            要求：分点说明、给出具体建议。
            """;

    @GetMapping("/template")
    public String template(@RequestParam(value = "question", defaultValue = "中午吃什么") String question,
                           @RequestParam(value = "topic", defaultValue = "食堂") String topic) {
        // 用模板 + 变量生成最终 Prompt
        Prompt prompt = PromptTemplate.builder()
                .template(TOPIC_TEMPLATE)
                .build()
                .create(Map.of("question", question, "topic", topic));

        return chatClient
                .prompt(prompt)
                .call()
                .content();
    }
}
