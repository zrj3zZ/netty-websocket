package com.example.demo;

import com.example.demo.config.NettyServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
        try {
            new NettyServer(12345).start();
        }catch(Exception e) {
            System.out.println("NettyServerError:"+e.getMessage());
        }
    }

}
