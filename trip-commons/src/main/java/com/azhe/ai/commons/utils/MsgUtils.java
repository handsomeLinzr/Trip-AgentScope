package com.azhe.ai.commons.utils;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;

import java.util.List;

/**
 * @author linzherong
 * @date 2026/7/29 13:16
 */
public class MsgUtils {

    private MsgUtils() {}

    /**
     * 文本消息封装
     * @param msg
     * @return
     */
    public static Msg buildText(String msg) {
        return Msg.builder()
                .role(MsgRole.USER)
                .content(List.of(TextBlock.builder()
                                .text(msg)
                        .build()))
                .build();
    }

}
