package foatto.core_server.ds.netty

import foatto.core.util.getCurrentTimeInt
import foatto.sql.AdvancedConnection
import foatto.sql.CoreAdvancedConnection
import foatto.sql.DBConfig
import io.netty.channel.ChannelHandlerContext
import java.io.File
import java.time.ZoneId

abstract class AbstractTelematicNettyHandler(
    val dbConfig: DBConfig,
    val dirSessionLog: File,
    val dirJournalLog: File,
) : AbstractNettyHandler() {

//    protected lateinit var conn: CoreAdvancedConnection

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected val zoneId: ZoneId = ZoneId.systemDefault()

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- тип прибора - должен переопределяться в наследниках
    protected var deviceType = -1

    //--- время начала сессии
    protected var begTime = 0

    //--- запись состояния сессии
    protected var status = ""

    //--- текст ошибки
    protected var errorText = ""

    //--- количество записанных блоков данных (например, точек)
    protected var dataCount = 0

    //--- количество считанных блоков данных (например, точек)
    protected var dataCountAll = 0

    //--- время первого и последнего блока данных (например, точки)
    protected var firstPointTime = 0
    protected var lastPointTime = 0

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun handlerAdded(ctx: ChannelHandlerContext?) {
        super.handlerAdded(ctx)

//        conn = AdvancedConnection(dbConfig)

        begTime = getCurrentTimeInt()
        status += " Init;"
    }

    override fun handlerRemoved(ctx: ChannelHandlerContext?) {
//        conn.close()

        begTime = 0
        status = ""
        
        super.handlerRemoved(ctx)
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)

        status += " Start;"

        //        final ByteBuf time = ctx.alloc().buffer(4); // (2)
//        time.writeInt((int) (System.currentTimeMillis() / 1000L + 2208988800L));
//
//        final ChannelFuture f = ctx.writeAndFlush(time); // (3)
//        f.addListener(new ChannelFutureListener() {
//            @Override
//            public void operationComplete(ChannelFuture future) {
//                assert f == future;
//                ctx.close();
//            }
//        }); // (4)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        super.channelRead(ctx, msg)

        if (begTime == 0) {
            begTime = getCurrentTimeInt()
        }
    }
}