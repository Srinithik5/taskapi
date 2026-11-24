package com.srinithi.taskapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TaskApiApplication {

    public static void main(String[] args) {
        // Force UTC regardless of the host OS's timezone. Without this, the Postgres
        // JDBC driver tries to set the DB session timezone to the JVM default - on this
        // machine that's the legacy alias "Asia/Calcutta", which Postgres 16 rejects
        // outright, crashing startup. Storing/comparing everything in UTC also avoids
        // subtle bugs if this app is ever deployed to a server in a different timezone.
        System.setProperty("user.timezone", "UTC");
        SpringApplication.run(TaskApiApplication.class, args);
    }
}
