package com.example.demo.netty;

import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName UserMap
 * @Author sxwddn
 * @Date 2019/12/6 13:26
 **/
public class UserMap {
    private HashMap<String, Channel> users = new HashMap();
    private static UserMap instance;

    public static UserMap getInstance () {
        if (instance == null) {
            instance = new UserMap();
        }
        return instance;
    }

    private UserMap () {

    }
    public void addUser(String userId, Channel ch) {
        this.users.put(userId, ch);
    }

    public Channel getUser (String userId) {
        return this.users.get(userId);
    }

    public void deleteUser (Channel ch) {
        for (Map.Entry<String, Channel> map: users.entrySet()) {
            if (map.getValue() == ch) {
                users.remove(map.getKey());
                break;
            }
        }
    }
}
