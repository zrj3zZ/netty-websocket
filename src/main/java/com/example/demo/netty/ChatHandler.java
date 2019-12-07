package com.example.demo.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * @ClassName ChatHandler
 * @Author sxwddn
 * @Date 2019/12/6 13:54
 **/
public class ChatHandler extends SimpleChannelInboundHandler {

    public static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    public static UserMap usermap = UserMap.getInstance();
    /**
     * 每当从服务端收到新的客户端连接时，客户端的 Channel 存入ChannelGroup列表中，并通知列表中的其他客户端 Channel
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel incoming = ctx.channel();
        for (Channel channel : channels) {
            channel.writeAndFlush("[SERVER] - " + incoming.remoteAddress() + " 加入\n");
        }
        channels.add(ctx.channel());
    }

    /**
     * 每当从服务端收到客户端断开时，客户端的 Channel 移除 ChannelGroup 列表中，并通知列表中的其他客户端 Channel
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        Channel incoming = ctx.channel();
        for (Channel channel : channels) {
            channel.writeAndFlush("[SERVER] - " + incoming.remoteAddress() + " 离开\n");
        }
        channels.remove(ctx.channel());
    }

    /**
     * 会话建立时
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception { // (5)
        Channel incoming = ctx.channel();
        System.out.println("ChatClient:"+incoming.remoteAddress()+"在线");
    }

    /**
     * 会话结束时
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception { // (6)
        Channel incoming = ctx.channel();
        System.out.println("ChatClient:"+incoming.remoteAddress()+"掉线");
        //清除离线用户
        this.usermap.deleteUser(incoming);
    }

    /**
     * 出现异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (7)
        Channel incoming = ctx.channel();
        System.out.println("ChatClient:"+incoming.remoteAddress()+"异常");
        // 当出现异常就关闭连接
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * 读取客户端发送的消息，并将信息转发给其他客户端的 Channel。
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object  request) throws Exception {
        if (request instanceof FullHttpRequest) { //是http请求
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,HttpResponseStatus.OK , Unpooled.wrappedBuffer("Hello netty"
                    .getBytes()));
            response.headers().set("Content-Type", "text/plain");
            response.headers().set("Content-Length", response.content().readableBytes());
            response.headers().set("connection", HttpHeaderValues.KEEP_ALIVE);
            ctx.channel().writeAndFlush(response);
        } else if (request instanceof TextWebSocketFrame) { // websocket请求
            //此处id为neety自动分配给每个对话线程的id,有两种,一个长id一个短id,长id唯一,短id可能会重复
            String userId = ctx.channel().id().asLongText();
            //客户端发送过来的消息
            String msg = ((TextWebSocketFrame)request).text();
            System.out.println("收到客户端"+userId+":"+msg);

            //发送消息给所有客户端  群聊
            //channels.writeAndFlush(new TextWebSocketFrame(msg));



            // 邦定user和channel
            // 定义每个上线用户主动发送初始化信息过来,携带自己的name,然后完成绑定  模型  init:[usrname]
            // 实际场景中应该使用user唯一id
            if (msg.indexOf("init") != -1) {
                String userNames[] = msg.split(":");
                if ("init".equals(userNames[0])) { // 记录新的用户
                    this.usermap.addUser(userNames[1].trim(), ctx.channel());
                    ctx.channel().writeAndFlush(new TextWebSocketFrame("success"));
                }
            }


            //搜索在线用户      消息模型  search:[username]
            if (msg.indexOf("search") != -1) {
                Channel ch = this.usermap.getUser(msg.split(":")[1].trim());
                if (ch != null) { //此用户存在
                    ctx.channel().writeAndFlush(new TextWebSocketFrame("search:"+msg.split(":")[1].trim()+":已找到"));
                } else { // 此用户不存在
                    ctx.channel().writeAndFlush(new TextWebSocketFrame("search:"+msg.split(":")[1].trim()+":未找到"));
                }

            }

            //发送消息给指定的用户    消息模型  me:to:[msg]
            if (msg.split(":").length == 3) {  //判断是单聊消息
                this.usermap.getUser(msg.split(":")[1].trim()).writeAndFlush(new TextWebSocketFrame(msg));
            }

            //ctx.channel().writeAndFlush(new TextWebSocketFrame(((TextWebSocketFrame)request).text()));
        }
    }

}