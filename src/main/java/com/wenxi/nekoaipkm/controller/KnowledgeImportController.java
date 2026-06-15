package com.wenxi.nekoaipkm.controller;

import com.wenxi.nekoaipkm.model.vo.ImportResponse;
import com.wenxi.nekoaipkm.service.KnowledgeImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识导入接口，负责触发本地笔记扫描
 *
 * @RequiredArgsConstructor 是 Lombok 提供的一个注解，用于 自动生成包含 final 或 @NonNull 修饰字段的构造方法，
 * 从而减少样板代码（boilerplate code），尤其适合 Spring 构造器注入 场景。
 */

@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class KnowledgeImportController {

    // 此处通过@RequiredArgsConstructor 注解构造器自动注入
    private final KnowledgeImportService knowledgeImportService;

    /**
     * 扫描配置目录下的 MarkDown 文档并导入知识库
     *
     * @return  导入统计结果
     */
    @PostMapping("/import/scan")
    public ImportResponse scanAndImport() {
        return knowledgeImportService.scanAndImport();
    }

}
