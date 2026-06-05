package com.seifmolabs;

import com.seifmolabs.api.ApproxApi;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
public class App {
    public static void main(String[] args) {
        var app = Javalin.create(config -> {

            config.staticFiles.add("/public", Location.CLASSPATH);
//            config.bundledPlugins.enableCors(cors ->
//                    cors.addRule(it -> it.anyHost())
//            );
        }).start(7070);

        ApproxApi.register(app);

        System.out.println("http://localhost:7070");
    }
}