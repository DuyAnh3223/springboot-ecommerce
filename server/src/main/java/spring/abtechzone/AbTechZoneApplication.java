package spring.abtechzone;

import java.time.ZoneId;
import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AbTechZoneApplication {

    public static void main(String[] args) {
        System.out.println("ZoneId = " + ZoneId.systemDefault());
        System.out.println("TimeZone = " + TimeZone.getDefault().getID());
        SpringApplication.run(AbTechZoneApplication.class, args);
    }
}
