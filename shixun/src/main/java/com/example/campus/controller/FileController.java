package com.example.campus.controller;

import com.example.campus.common.BusinessException;
import com.example.campus.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传。
 *
 * 申诉的证据图、实名认证的学生证照片都走这里；返回的是 /files/yyyy/MM/dd/xxx.png
 * 这样的相对路径，前端拿到后拼上自己的 base-url 即可显示（WebConfig 里把这个
 * 前缀映射成了静态资源）。
 *
 * 只存路径不存文件本身到数据库：appeals.evidence_urls / users.student_cert_url
 * 本来就是 varchar/text，存 URL 字符串，和 orders.image_urls 的做法保持一致。
 */
@Slf4j
@RestController
@RequestMapping("/files")
public class FileController {

    /** 允许的扩展名。不做「按文件头判断真实类型」的深度校验，实训场景够用 */
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");

    /** 单张上限 5MB，对应 PRD 4.1「图片上传大小限制 < 5MB」 */
    private static final long MAX_SIZE = 5 * 1024 * 1024L;

    @Value("${campus.upload.dir:uploads}")
    private String uploadDir;

    @Value("${campus.upload.url-prefix:/files}")
    private String urlPrefix;

    /**
     * 上传一张图片。
     *
     * 访问：POST /files/upload （form-data，字段名 file）
     * 返回：{ "url": "/files/2026/10/15/xxx.png", "name": "原文件名", "size": 12345 }
     *
     * 这个接口是 POST，不在 JwtInterceptor 的静态资源白名单里（那里只放行 GET），
     * 所以必须带 token 才能传。
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        // 容器层面还有一道 spring.servlet.multipart.max-file-size=5MB 兜着，
        // 但超过时容器直接抛异常、提示不友好，所以这里再显式判一次。
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("图片不能超过 5MB");
        }

        String originalName = file.getOriginalFilename();
        String ext = extensionOf(originalName);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("只支持 jpg / jpeg / png / webp 格式的图片");
        }

        // 按日期分目录，避免所有图片堆在一个文件夹里
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        // 用 UUID 而不是原文件名：原文件名可能重复、也可能带路径分隔符
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;

        Path dir = Paths.get(uploadDir, datePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(fileName));
        } catch (IOException e) {
            log.error("图片保存失败, dir={}, name={}", dir, fileName, e);
            throw new BusinessException("图片上传失败，请重试");
        }

        String url = urlPrefix + "/" + datePath + "/" + fileName;
        log.info("图片上传成功: {} -> {}", originalName, url);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("url", url);
        data.put("name", originalName);
        data.put("size", file.getSize());
        return Result.success(data);
    }

    /** 取小写扩展名；没有扩展名返回空串（会被白名单拒掉） */
    private String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase();
    }
}
