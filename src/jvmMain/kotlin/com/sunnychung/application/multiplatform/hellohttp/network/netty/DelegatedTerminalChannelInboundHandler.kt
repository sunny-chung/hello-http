package com.sunnychung.application.multiplatform.hellohttp.network.netty

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandler

class DelegatedTerminalChannelInboundHandler(val delegations: List<ChannelInboundHandler>) : ChannelInboundHandler {
    override fun handlerAdded(ctx: ChannelHandlerContext?) {
        delegations.forEach { it.handlerAdded(ctx) }
    }

    override fun handlerRemoved(ctx: ChannelHandlerContext?) {
        delegations.forEach { it.handlerRemoved(ctx) }
    }

    @Deprecated("Deprecated in Java")
    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
//        delegations.forEach { it.exceptionCaught(ctx, cause) }
    }

    override fun channelRegistered(ctx: ChannelHandlerContext?) {
        delegations.forEach { it.channelRegistered(ctx) }
    }

    override fun channelUnregistered(ctx: ChannelHandlerContext?) {
        delegations.forEach { it.channelUnregistered(ctx) }
    }

    override fun channelActive(ctx: ChannelHandlerContext?) {
        delegations.forEach { it.channelActive(ctx) }
    }

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        delegations.forEach { it.channelInactive(ctx) }
    }

    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        delegations.forEach { it.channelRead(ctx, msg) }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext?) {
        delegations.forEach { it.channelReadComplete(ctx) }
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext?, evt: Any?) {
        delegations.forEach { it.userEventTriggered(ctx, evt) }
    }

    override fun channelWritabilityChanged(ctx: ChannelHandlerContext?) {
        delegations.forEach { it.channelWritabilityChanged(ctx) }
    }
}
