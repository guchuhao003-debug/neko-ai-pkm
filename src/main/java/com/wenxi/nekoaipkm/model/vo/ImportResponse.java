package com.wenxi.nekoaipkm.model.vo;

/**
 * 知识导入响应，用于返回本次扫描的处理数量
 *
 * @param scannedFiles  扫描到的 MarkDown 文件数
 * @param importedFiles 实际导入或更新的文件数
 * @param skippedFiles  未变化而跳过的文件数
 */
public record ImportResponse(
        int scannedFiles,
        int importedFiles,
        int skippedFiles
) {
}
