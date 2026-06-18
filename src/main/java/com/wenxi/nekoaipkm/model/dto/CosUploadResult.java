package com.wenxi.nekoaipkm.model.dto;

/**
 * COS 上传结果
 *
 * @param bucket    存储桶名称
 * @param objectKey 对象 KEY
 * @param cosUri    COS 内部地址
 * @param originalFileName  原始文件名
 * @param contentType   文件类型
 */
public record CosUploadResult(

        String bucket,
        String objectKey,
        String cosUri,
        String originalFileName,
        String contentType
) {}
