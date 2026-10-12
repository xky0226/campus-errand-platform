package com.example.exam.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 任务五：Advisor 拦截链。
 * 通过 .defaultAdvisors(...) 把三个拦截器挂到 ChatClient 上，所有接口统一生效。
 *
 * 执行顺序由各自的 getOrder() 决定，数值越小越先执行：
 *   SimpleLoggerAdvisor   order = 0（框架默认）→ 最先打印请求
 *   SafeGuardAdvisor      order = 1            → 拦截敏感词，命中就不再往下走
 *   RewritePromptAdvisor  order = 2            → 改写用户问题（要求排在 SafeGuardAdvisor 之后）
 */
@Configuration
public class AiConfig {

    /**
     * 敏感词列表（自定义，注意文明）—— 数量 7 个，满足「不少于 5 个」。
     * 选的都是和校园场景相关的违规需求，避免误伤正常的校园提问。
     */
    private static final List<String> SENSITIVE_WORDS = List.of(
            "代写", "刷单", "赌博", "色情", "暴力", "诈骗", "毒品");

    /** 命中敏感词后的兜底回复，直接返回，不会请求大模型 */
    private static final String SAFE_GUARD_RESPONSE =
            "抱歉，您的问题里包含不适合讨论的内容，我无法对此进行回复。"
                    + "您可以换一种说法，或者问我校园学习、校园生活方面的其他问题。";

    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient
                .builder(chatModel)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),                                       // ① 日志
                        new SafeGuardAdvisor(SENSITIVE_WORDS, SAFE_GUARD_RESPONSE, 1),   // ② 敏感词过滤
                        new RewritePromptAdvisor()                                       // ③ 自定义：改写提示词
                )
                .build();
    }
}
