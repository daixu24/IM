package com.crazymakercircle.im.common.codec;

import com.crazymakercircle.im.common.ProtoInstant;
import com.crazymakercircle.im.common.bean.msg.ProtoMsg;
import com.crazymakercircle.im.common.exception.InvalidFrameException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
/**
 * create by 尼恩 @ 疯狂创客圈
 **/


/**
 * create by 尼恩 @ 疯狂创客圈
 * <p>
 * 解码器
 */

@Slf4j
//解码器   将二进制编成字符
public class ProtobufDecoder extends ByteToMessageDecoder
//这种bytetomessage需要判断字符长度
{

    //入参是二进制流，出参是object
    @Override
    protected void decode(ChannelHandlerContext ctx,
                          ByteBuf in,
                          List<Object> out) throws Exception
    {
        // 标记一下当前的readIndex的位置
        in.markReaderIndex();
        // 判断包头长度
        if (in.readableBytes() < 8)
        {
            return;
        }
        //读取魔数
        short magic = in.readShort();
        if (magic != ProtoInstant.MAGIC_CODE)
        {
            String error = "客户端口令不对:" + ctx.channel().remoteAddress();
            throw new InvalidFrameException(error);
        }
        //读取版本
        short version = in.readShort();
        // 读取传送过来的消息的长度。
        int length = in.readInt();

        // 长度如果小于0
        if (length < 0)
        {// 非法数据，关闭连接
            ctx.close();
        }

        //说明消息还没完全到达
        //缓冲的指针决定了下一次读取哪个，底层的来了就进入用户缓冲，但是指针还是在这里
        if (length > in.readableBytes())
        {// 读到的消息体长度如果小于传送过来的消息长度
            // 重置读取位置
            in.resetReaderIndex();
            return;
        }


        byte[] array;
        if (in.hasArray())
        {
            //堆缓冲
//            ByteBuf slice = in.slice();
            //小伙伴 calvin 发现的bug，这里指正读取  length 长度
            //切片和原来的缓冲指针相同
            ByteBuf slice = in.slice(in.readerIndex(),length);  //切片
            array = slice.array();  //这里指针到了末端
            in.retain();
            //增加一次引用计数   成对使用  不过可以不用的

        } else
        {
            //直接缓冲
            array = new byte[length];
            //当前的读指针
            in.readBytes(array, 0, length);
        }

//        if(in.refCnt()>0)
//        {
////            log.debug("释放临时缓冲");
//            in.release();
//        }

        // 字节转成对象
        ProtoMsg.Message outmsg =
                ProtoMsg.Message.parseFrom(array);
        //释放 ？？？？？
        if (in.hasArray()) {
            in.release();
        }
        if (outmsg != null)
        {
            // 获取业务消息 加入object
            out.add(outmsg);
        }

    }
}
