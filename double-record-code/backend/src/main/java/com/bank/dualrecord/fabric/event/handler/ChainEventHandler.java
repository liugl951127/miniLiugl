package com.bank.dualrecord.fabric.event.handler;

import com.bank.dualrecord.fabric.event.ChainEventListener;

/**
 * 链码事件处理器接口(责任链模式)
 */
public interface ChainEventHandler {

    /**
     * 判断是否处理该事件名
     */
    boolean supports(String eventName);

    /**
     * 处理事件
     */
    void handle(ChainEventListener event);

    /**
     * 优先级(数字越小越先执行)
     */
    default int order() {
        return 100;
    }
}
