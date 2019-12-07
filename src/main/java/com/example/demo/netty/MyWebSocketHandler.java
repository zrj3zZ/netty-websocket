package com.example.demo.netty;

import com.example.demo.config.MyChannelHandlerPool;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName MyWebSocketHandler
 * @Author sxwddn
 * @Date 2019/12/5 12:09
 **/
public class MyWebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    public static UserMap usermap = UserMap.getInstance();

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("与客户端建立连接，通道开启！");
        //添加到channelGroup通道组
        MyChannelHandlerPool.channelGroup.add(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("与客户端断开连接，通道关闭！");
        //移除channelGroup 通道组
        MyChannelHandlerPool.channelGroup.remove(ctx.channel());
        Channel incoming = ctx.channel();
        this.usermap.deleteUser(incoming);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //首次连接是FullHttpRequest，处理参数 by zhengkai.blog.csdn.net
        if (null != msg && msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            String uri = request.uri();
            Map paramMap=getUrlParams(uri);
            System.out.println("接收到的参数是："+paramMap.get("uid"));
            //如果url包含参数，需要处理
            if(uri.contains("?")){
                String newUri=uri.substring(0,uri.indexOf("?"));
                request.setUri(newUri);
            }
        }else if(msg instanceof TextWebSocketFrame){
            //正常的TEXT消息类型
            String content = ((TextWebSocketFrame)msg).text();
//            System.out.println("客户端收到服务器数据：" +content);
//            sendAllMessage(content);
            //------------------------------------------------
            String userId = ctx.channel().id().asLongText();
            System.out.println("收到客户端"+userId+":"+msg);
            // 邦定user和channel
            // 定义每个上线用户主动发送初始化信息过来,携带自己的name,然后完成绑定  模型  init:[usrname]
            // 实际场景中应该使用user唯一id
            if (content.indexOf("init") != -1) {
                String userNames[] = content.split(":");
                if ("init".equals(userNames[0])) { // 记录新的用户
                    this.usermap.addUser(userNames[1].trim(), ctx.channel());
                    ctx.channel().writeAndFlush(new TextWebSocketFrame("success"));
                }
            }

            //搜索在线用户      消息模型  search:[username]
            if (content.indexOf("search") != -1) {
                Channel ch = this.usermap.getUser(content.split(":")[1].trim());
                if (ch != null) { //此用户存在
                    ctx.channel().writeAndFlush(new TextWebSocketFrame("search:"+content.split(":")[1].trim()+":已找到"));
                } else { // 此用户不存在
                    ctx.channel().writeAndFlush(new TextWebSocketFrame("search:"+content.split(":")[1].trim()+":不存在"));
                }
            }

            //发送消息给指定的用户    消息模型  me:to:[msg]
            if (content.split(":").length == 3) {  //判断是单聊消息
                this.usermap.getUser(content.split(":")[1].trim()).writeAndFlush(new TextWebSocketFrame(content));
            }else{
                if (content.indexOf("search") == -1) {
                    MyChannelHandlerPool.channelGroup.writeAndFlush( new TextWebSocketFrame(content));
                }
            }
        }
        super.channelRead(ctx, msg);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, TextWebSocketFrame textWebSocketFrame) throws Exception {

    }
    /**
     * 心跳检测的超时时会触发
     * @param ctx
     * @param evt
     * @throws Exception
     */
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {  //读取心跳超时后，会将此channel连接断开
                System.out.println("trigger channel =" + ctx.channel());
                ctx.close();  //如果超时，关闭这个通道
            }
        } else if (evt instanceof SslHandshakeCompletionEvent) {
            System.out.println("ssl handshake done");
            //super.userEventTriggered(ctx,evt);
        }
    }




    private void sendAllMessage(String message){
        //收到信息后，群发给所有channel
        MyChannelHandlerPool.channelGroup.writeAndFlush( new TextWebSocketFrame(message));
    }

    private static Map getUrlParams(String url){
        Map<String,String> map = new HashMap<>();
        url = url.replace("?",";");
        if (!url.contains(";")){
            return map;
        }
        if (url.split(";").length > 0){
            String[] arr = url.split(";")[1].split("&");
            for (String s : arr){
                String key = s.split("=")[0];
                String value = s.split("=")[1];
                map.put(key,value);
            }
            return  map;

        }else{
            return map;
        }
    }
}
