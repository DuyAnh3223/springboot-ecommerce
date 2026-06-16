package spring.abtechzone.config;

import com.nimbusds.jose.JOSEException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import spring.abtechzone.dto.auth.IntrospectRequest;
import spring.abtechzone.service.AuthService;

import javax.crypto.spec.SecretKeySpec;
import java.text.ParseException;
import java.util.Objects;

@Component
public class CustomJwtDecoder implements JwtDecoder {

    @Value("${jwt.signerKey}")
    private String jwtSignerKey;

    @Autowired
    AuthService authService;

    NimbusJwtDecoder nimbusJwtDecoder=null;

    @Override
    public Jwt decode(String token) throws JwtException {

        try {
            var response = authService.introspect(IntrospectRequest.builder().token(token).build());
            if(!response.isValid()){
                throw new JwtException("Invalid token");
            }
        } catch (JOSEException | ParseException e) {
            throw new JwtException(e.getMessage());
        }

        if(Objects.isNull(nimbusJwtDecoder)) {
            SecretKeySpec secretKeySpec = new SecretKeySpec(jwtSignerKey.getBytes(), "HS512");

            nimbusJwtDecoder = NimbusJwtDecoder
                                    .withSecretKey(secretKeySpec)
                                    .macAlgorithm(MacAlgorithm.HS512)
                                    .build();
        }
        System.out.println(nimbusJwtDecoder);

        return nimbusJwtDecoder.decode(token);
    }
}
