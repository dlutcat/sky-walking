package com.a.eye.skywalking.protocol.util;

import com.a.eye.skywalking.conf.Config;
import com.a.eye.skywalking.model.ContextData;
import com.a.eye.skywalking.context.CurrentThreadSpanStack;
import com.a.eye.skywalking.model.Identification;
import com.a.eye.skywalking.protocol.Span;

public final class ContextGenerator {
    /**
     * 利用本地ThreadLocal的信息创建Context，主要用于非跨JVM的操作
     *
     * @param id 视点，业务数据等信息
     * @return
     */
    public static Span generateSpanFromThreadLocal(Identification id) {
        Span spanData = getSpanFromThreadLocal();
        spanData.setStartDate(System.currentTimeMillis());
        spanData.setViewPointId(id.getViewPoint());
        return spanData;
    }

    /**
     * 利用传入的Context对象，来构建相对应的Context信息，主要用于跨JVM的操作信息
     * 跨JVM会产生两条记录。
     *
     * @param context
     * @return
     */
    public static Span generateSpanFromContextData(ContextData context, Identification id) {
        Span spanData = CurrentThreadSpanStack.peek();
        if (context != null && !StringUtil.isEmpty(context.getTraceId()) && spanData == null){
            spanData = new Span(context.getTraceId(), context.getParentLevel(), context.getLevelId(), Config.SkyWalking.APPLICATION_CODE, Config.SkyWalking.USER_ID);
        }else{
            spanData = getSpanFromThreadLocal();
        }
        spanData.setStartDate(System.currentTimeMillis());
        spanData.setViewPointId(id.getViewPoint());
        return spanData;
    }

    private static Span getSpanFromThreadLocal() {
        Span span;
        // 1.获取Context，从ThreadLocal栈中获取中
        final Span parentSpan = CurrentThreadSpanStack.peek();
        // 2 校验Context，Context是否存在
        if (parentSpan == null) {
            // 不存在，新创建一个Context
            span = new Span(TraceIdGenerator.generate(), Config.SkyWalking.APPLICATION_CODE, Config.SkyWalking.USER_ID);
        } else {

            // 根据ParentContextData的TraceId和RPCID
            // LevelId是由SpanNode类的nextSubSpanLevelId字段进行初始化的.
            // 所以在这里不需要初始化
            span = new Span(parentSpan.getTraceId(), Config.SkyWalking.APPLICATION_CODE, Config.SkyWalking.USER_ID);
            if (!StringUtil.isEmpty(parentSpan.getParentLevel())) {
                span.setParentLevel(parentSpan.getParentLevel() + "." + parentSpan.getLevelId());
            } else {
                span.setParentLevel(String.valueOf(parentSpan.getLevelId()));
            }
        }
        return span;
    }

}
