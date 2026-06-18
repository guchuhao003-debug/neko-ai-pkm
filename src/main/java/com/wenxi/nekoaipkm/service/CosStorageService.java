package com.wenxi.nekoaipkm.service;

import cn.hutool.core.util.IdUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.wenxi.nekoaipkm.model.dto.CosUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.LocalDate;

/**
 * COS 文件存储服务，负责保存用户上传的原始文档
 */
@Service
@RequiredArgsConstructor
public class CosStorageService {

    private final COSClient cosClient;

    @Value("${pkm.cos.bucket}")
    private String bucket;

    @Value("${pkm.cos.key-prefix:pkm/uploads/}")
    private String keyPrefix;

    /**
     * 上传 Markdown 文件到 COS
     * @param file  上传文件
     * @return  上传结果
     */
    public CosUploadResult uploadMarkDown(MultipartFile file) {
        // 校验文件信息
        validateMarkdownFile(file);

        String originalFileName = file.getOriginalFilename();
        String safeFileName = Paths.get(originalFileName).getFileName().toString();
        String objectKey = buildObjectKey(safeFileName);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(resolveContentType(file));

        try (InputStream inputStream = file.getInputStream()) {
            cosClient.putObject(bucket, objectKey, inputStream, metadata);
        } catch (IOException e) {
            throw new IllegalStateException("上传文件到 COS 失败：" + safeFileName, e);
        }

        return new CosUploadResult(
                bucket,
                objectKey,
                "cos://" + bucket + "/" + objectKey,
                safeFileName,
                metadata.getContentType()
        );
    }



    /**
     * 构造 COS 对象 Key
     *
     * @param safeFileName  安全文件名
     * @return  对象 Key
     */
    private String buildObjectKey(String safeFileName) {
        String normalizedPrefix = keyPrefix.endsWith("/")
                ? keyPrefix
                : keyPrefix + "/";

        return normalizedPrefix
                + LocalDate.now()
                + "/"
                + IdUtil.fastSimpleUUID()
                + "-"
                + safeFileName;
    }


    /**
     * 校验上传文件信息
     *
     * @param file  上传文件
     */
    private void validateMarkdownFile(MultipartFile file) {
        // 校验文件信息
        if (file == null || file.isEmpty()) {
            throw new IllegalStateException("上传文件不能为空");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalStateException("上传文件名不能为空");
        }

        String lowerFileName = originalFileName.toLowerCase();
        if (!lowerFileName.endsWith(".md") && !lowerFileName.endsWith(".markdown")) {
            throw new IllegalStateException("当前仅支持 MarkDown 文件");
        }
    }

    /**
     * 解析文件 Content-Type
     *
     * @param file  上传文件
     * @return  Content-Type
     */
    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType == null ? "text/markdown" : contentType;
    }

}
