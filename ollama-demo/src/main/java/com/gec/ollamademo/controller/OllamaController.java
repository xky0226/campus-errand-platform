package com.gec.ollamademo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 本地 Ollama 模型的对话接口。
 *
 * 用的是 AiConfig 里那个叫 ollamaChatClient 的 ChatClient Bean。
 * 当项目里存在多个 ChatClient 类型的 Bean 时，可以按字段名和 Bean 名匹配注入，
 * 也可以用 @Qualifier("ollamaChatClient") 明确指定 —— 这里字段名就是 ollamaChatClient，
 * 所以不用再写 @Qualifier 也能注入对。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/ollama")
public class OllamaController {

    // 注入名为 ollamaChatClient 的 Bean
    private final ChatClient ollamaChatClient;

    /**
     * 访问：http://localhost:8080/ollama/chat?question=你是谁
     *
     * 回答完全来自本地模型，即使断网也能工作。
     */
    @GetMapping("/chat")
    public Flux<String> chat(@RequestParam(value = "question", defaultValue = "你是谁") String question) {
        return ollamaChatClient
                .prompt()
                .user(question)
                .stream()
                .content();
    }
}
