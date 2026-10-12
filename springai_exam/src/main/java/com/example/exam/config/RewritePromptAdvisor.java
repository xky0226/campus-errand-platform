package com.example.exam.config;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.List;
import java.util.Map;

/**
 * 任务五-③：自定义 Advisor —— 在请求发给模型之前，用 PromptTemplate 把用户的原始问题
 * 改写成「先给结论、再分点解释、Markdown 排版」的提问框架。
 *
 * 实现 BaseAdvisor 需要重写 before / after / getOrder 三个方法。
 *
 * 注意：作业讲义里写的 import 是 org.springframework.ai.chat.client.advisor.BaseAdvisor，
 * 在 Spring AI 2.0.0 里这个类实际位于 org.springframework.ai.chat.client.advisor.api 包下，
 * 照讲义原样写会编译不过，这里用的是正确包名。
 */
public class RewritePromptAdvisor implements BaseAdvisor {

    /** 改写用的模板，{question} 是占位符 */
    private static final String REWRITE_TEXT = """
            请按「先给结论，再分点解释」的结构回答下面的问题。

            问题：{question}

            回答要求：
            1. 第一行先用一句话直接给出结论；
            2. 再用 1. 2. 3. 分点解释，每点都要具体；
            3. 使用 Markdown 格式排版；
            4. 语言通俗易懂，必要时举例说明。
            """;

    /** 请求发送前执行：可以修改请求内容 */
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        Prompt prompt = chatClientRequest.prompt();

        // 只替换「用户消息」的内容，保留 System 消息。
        // 讲义示例是把整个 prompt 换掉，那样会把接口里 .system(...) 设定的人设一起丢掉，
        // 校园百事通的角色和越界拒绝都会失效，所以这里改成只重写最后一条用户消息。
        List<Message> messages = prompt.getInstructions().stream()
                .map(message -> message instanceof UserMessage userMessage
                        ? (Message) new UserMessage(rewrite(userMessage.getText()))
                        : message)
                .toList();

        Prompt newPrompt = prompt.mutate().messages(messages).build();
        return chatClientRequest.mutate().prompt(newPrompt).build();
    }

    /** 用 PromptTemplate 把原始问题包进分析框架 */
    private String rewrite(String question) {
        return PromptTemplate.builder()
                .template(REWRITE_TEXT)
                .build()
                .render(Map.of("question", question));
    }

    /** 响应返回后执行：这里原样返回 */
    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    /** 执行顺序：数值越小越先执行。SafeGuardAdvisor 是 1，这里取 2，排在它之后 */
    @Override
    public int getOrder() {
        return 2;
    }
}
