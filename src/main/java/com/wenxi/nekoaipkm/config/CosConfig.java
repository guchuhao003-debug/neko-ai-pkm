package com.wenxi.nekoaipkm.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云 COS 客户端配置
 *
 */
@Configuration
public class CosConfig {

    @Value("${pkm.cos.secret-id}")
    private String secretId;

    @Value("${pkm.cos.secret-key}")
    private String secretKey;

    @Value("${pkm.cos.region}")
    private String region;

    /**
     * 创建 COSClient, 用于上传和读取对象存储文件
     *
     * @return  COS 客户端
     */
    @Bean(destroyMethod = "shutdown")
    public COSClient cosClient() {
        COSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        return new COSClient(credentials, clientConfig);
    }
}
