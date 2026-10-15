package com.gec.ollamademo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ollama 本地模型的 ChatClient。
 *
 * 注意注入的是 OllamaChatModel（本地模型），跟云端那套 OpenAiChatModel 区分开。
 * 两者可以在同一个项目里并存：只要分别声明不同的 ChatClient Bean，
 * 用的时候按 Bean 名或 @Qualifier 指定即可（见讲义 5-4 模块 07 常见问题 Q4）。
 */
@Configuration
public class AiConfig {

    /**
     * 本地 Ollama 模型客户端。
     * Bean 名就是方法名 ollamaChatClient，注入时按字段名匹配即可。
     */
    @Bean
    public ChatClient ollamaChatClient(OllamaChatModel chatModel) {
        return ChatClient
                .builder(chatModel)
                .build();
    }
}
