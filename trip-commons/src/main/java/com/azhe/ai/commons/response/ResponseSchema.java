package com.azhe.ai.commons.response;

import lombok.Data;

/**
 * 输出格式
 * @author linzherong
 * @date 2026/7/30 18:46
 */
@Data
public class ResponseSchema {

    /**
     *  城市
     */
    public String city;

    /**
     *  旅程规划
     */
    public String plan;

    public ResponseSchema() {}

}
