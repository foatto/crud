package foatto.core_server.ds.netty

import foatto.core.util.AdvancedLogger
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

abstract class AbstractNettyHandler : ChannelInboundHandlerAdapter() {

    override fun handlerAdded(ctx: ChannelHandlerContext?) {
        AdvancedLogger.debug("Handler added.")
    }

    override fun handlerRemoved(ctx: ChannelHandlerContext?) {
        AdvancedLogger.debug("Handler removed.")
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        AdvancedLogger.debug("Channel active.")
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        AdvancedLogger.error(cause ?: Throwable("${this.javaClass.name}: cause is null."))
        ctx?.close()
    }
}