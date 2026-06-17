package com.signatureapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.jwt.secret=dGVzdHNlY3JldGtleWZvcmp3dHRlc3Rpbmd0ZXN0c2VjcmV0a2V5Zm9yandrZXk=",
    "app.jwt.expiration-ms=86400000",
    "spring.mail.host=localhost",
    "spring.mail.port=3025",
    "app.file.upload-dir=./test-uploads"
})
class SignatureAppApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring context loads without errors
    }
}
