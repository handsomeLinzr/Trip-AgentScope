package com.azhe.ai.trip.manager;

import com.azhe.ai.trip.manager.agent.ManagerAgent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author linzherong
 * @date 2026/7/28 00:22
 */
//@SpringBootApplication
public class ManagerApplication {

    public static void main(String[] args) {
//        SpringApplication.run(ManagerApplication.class, args);
        ManagerAgent managerAgent = new ManagerAgent();
        managerAgent.run("你好，帮我制定 2027 年元旦从广州到汕头的 3 天旅程。");
    }

}
