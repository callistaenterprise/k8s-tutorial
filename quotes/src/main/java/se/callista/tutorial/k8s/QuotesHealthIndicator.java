package se.callista.tutorial.k8s;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.stereotype.Component;

@Component
public class QuotesHealthIndicator extends AbstractHealthIndicator {

    public static boolean isAlive = true;
    
    @Override
    protected void doHealthCheck(Builder builder) throws Exception {
        if (isAlive) {
            builder.up();
        } else {
            builder.down();
        }
    }

}
