package com.example.demo.controller;

import com.example.demo.config.MyChannelHandlerPool;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName HelloWorldController
 * @Author sxwddn
 * @Date 2019/11/20 13:26
 **/
@RestController
public class HelloWorldController {
    @RequestMapping("/hello")
    public String hello(){
        MyChannelHandlerPool.channelGroup.writeAndFlush( new TextWebSocketFrame("hello"));
        return null;
    }
}
