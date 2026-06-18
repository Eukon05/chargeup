package ovh.eukon05.chargeup;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                description = "ChargeUP is a service that provides information about the current energy mix in Great Britain and predicts optimal charging windows for EVs based on clean energy availability.",
                title = "ChargeUP API",
                version = "1.0"
        )
)
public class ChargeupApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChargeupApplication.class, args);
    }

}
