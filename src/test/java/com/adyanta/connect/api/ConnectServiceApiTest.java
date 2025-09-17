package com.adyanta.connect.api;

import com.adyanta.connect.domain.dto.ConnectRequestDto;
import com.adyanta.connect.domain.enums.RequestType;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * REST Assured API tests for Connect Service
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ConnectServiceApiTest {

    @LocalServerPort
    private int port;

    private RequestSpecification requestSpec;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        RestAssured.basePath = "/api/v1";

        requestSpec = given()
                .header("X-Client-Id", "test-client")
                .header("X-Client-Secret", "test-secret")
                .header("X-API-Key", "test-api-key")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    @Test
    void processRequest_WithValidXmlPayload_ShouldReturnAccepted() {
        ConnectRequestDto request = ConnectRequestDto.builder()
                .requestType(RequestType.CUSTOMER_ONBOARDING)
                .payload("<customer><name>John Doe</name><email>john@example.com</email></customer>")
                .contentType("application/xml")
                .clientId("test-client")
                .requestId("test-request-123")
                .priority(5)
                .timeoutMs(300000L)
                .metadata(Map.of("source", "api-test"))
                .build();

        Response response = requestSpec
                .body(request)
                .when()
                .post("/connect/process")
                .then()
                .statusCode(202)
                .contentType(ContentType.JSON)
                .body("requestId", equalTo("test-request-123"))
                .body("status", equalTo("ACCEPTED"))
                .body("message", equalTo("Request accepted for processing"))
                .body("timestamp", notNullValue())
                .body("processingId", notNullValue())
                .extract()
                .response();

        assertNotNull(response);
    }

    @Test
    void processRequest_WithValidJsonPayload_ShouldReturnAccepted() {
        ConnectRequestDto request = ConnectRequestDto.builder()
                .requestType(RequestType.KYC_VERIFICATION)
                .payload("{\"customerId\": \"cust-789\", \"documents\": [{\"type\": \"passport\", \"number\": \"A1234567\"}]}")
                .contentType("application/json")
                .clientId("test-client")
                .requestId("test-request-456")
                .priority(8)
                .timeoutMs(600000L)
                .metadata(Map.of("source", "mobile-app"))
                .build();

        requestSpec
                .body(request)
                .when()
                .post("/connect/process")
                .then()
                .statusCode(202)
                .contentType(ContentType.JSON)
                .body("requestId", equalTo("test-request-456"))
                .body("status", equalTo("ACCEPTED"))
                .body("message", equalTo("Request accepted for processing"))
                .body("timestamp", notNullValue())
                .body("processingId", notNullValue());
    }

    @Test
    void processRequest_WithInvalidData_ShouldReturnBadRequest() {
        ConnectRequestDto request = ConnectRequestDto.builder()
                .payload("test")
                .contentType("application/xml")
                .build();

        requestSpec
                .body(request)
                .when()
                .post("/connect/process")
                .then()
                .statusCode(400);
    }

    @Test
    void processRequest_WithMissingHeaders_ShouldReturnUnauthorized() {
        ConnectRequestDto request = ConnectRequestDto.builder()
                .requestType(RequestType.CUSTOMER_ONBOARDING)
                .payload("<customer><name>John Doe</name></customer>")
                .contentType("application/xml")
                .clientId("test-client")
                .build();

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(request)
                .when()
                .post("/connect/process")
                .then()
                .statusCode(401);
    }

    @Test
    void processRequest_WithDifferentRequestTypes_ShouldWork() {
        RequestType[] requestTypes = {
                RequestType.CUSTOMER_ONBOARDING,
                RequestType.KYC_VERIFICATION,
                RequestType.DOCUMENT_PROCESSING,
                RequestType.COMPLIANCE_CHECK,
                RequestType.RISK_ASSESSMENT,
                RequestType.TRANSACTION_MONITORING,
                RequestType.GENERIC_PROCESSING
        };

        for (RequestType requestType : requestTypes) {
            ConnectRequestDto request = ConnectRequestDto.builder()
                    .requestType(requestType)
                    .payload("<test>data for " + requestType + "</test>")
                    .contentType("application/xml")
                    .clientId("test-client")
                    .requestId("test-" + requestType.name().toLowerCase())
                    .build();

            requestSpec
                    .body(request)
                    .when()
                    .post("/connect/process")
                    .then()
                    .statusCode(202)
                    .body("requestId", equalTo("test-" + requestType.name().toLowerCase()))
                    .body("status", equalTo("ACCEPTED"));
        }
    }

    @Test
    void getRequestStatus_WithValidRequestId_ShouldReturnStatus() {
        // First, create a request
        ConnectRequestDto request = ConnectRequestDto.builder()
                .requestType(RequestType.CUSTOMER_ONBOARDING)
                .payload("<customer><name>John Doe</name></customer>")
                .contentType("application/xml")
                .clientId("test-client")
                .requestId("status-test-123")
                .build();

        // Submit the request
        Response createResponse = requestSpec
                .body(request)
                .when()
                .post("/connect/process")
                .then()
                .statusCode(202)
                .extract()
                .response();

        String processingId = createResponse.jsonPath().getString("processingId");

        // Get status
        requestSpec
                .when()
                .get("/connect/status/status-test-123")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("requestId", equalTo("status-test-123"))
                .body("status", notNullValue())
                .body("message", notNullValue())
                .body("timestamp", notNullValue())
                .body("processingId", equalTo(processingId));
    }

    @Test
    void getRequestStatus_WithNonExistentRequestId_ShouldReturnNotFound() {
        requestSpec
                .when()
                .get("/connect/status/non-existent-request")
                .then()
                .statusCode(404)
                .contentType(ContentType.JSON)
                .body("requestId", equalTo("non-existent-request"))
                .body("status", equalTo("ERROR"))
                .body("message", equalTo("Request not found"));
    }

    @Test
    void health_ShouldReturnHealthStatus() {
        given()
                .when()
                .get("/connect/health")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("status", equalTo("UP"))
                .body("message", equalTo("Connect service is running"))
                .body("timestamp", notNullValue());
    }

    @Test
    void processRequest_ResponseShouldMatchJsonSchema() {
        ConnectRequestDto request = ConnectRequestDto.builder()
                .requestType(RequestType.CUSTOMER_ONBOARDING)
                .payload("<customer><name>John Doe</name></customer>")
                .contentType("application/xml")
                .clientId("test-client")
                .requestId("schema-test-123")
                .build();

        requestSpec
                .body(request)
                .when()
                .post("/connect/process")
                .then()
                .statusCode(202)
                .body(matchesJsonSchemaInClasspath("schemas/connect-response-schema.json"));
    }

    @Test
    void processRequest_WithLargePayload_ShouldHandleGracefully() {
        StringBuilder largeXml = new StringBuilder("<customer>");
        for (int i = 0; i < 1000; i++) {
            largeXml.append("<field").append(i).append(">value").append(i).append("</field").append(i).append(">");
        }
        largeXml.append("</customer>");

        ConnectRequestDto request = ConnectRequestDto.builder()
                .requestType(RequestType.DOCUMENT_PROCESSING)
                .payload(largeXml.toString())
                .contentType("application/xml")
                .clientId("test-client")
                .requestId("large-payload-test")
                .build();

        requestSpec
                .body(request)
                .when()
                .post("/connect/process")
                .then()
                .statusCode(202)
                .body("requestId", equalTo("large-payload-test"))
                .body("status", equalTo("ACCEPTED"));
    }

    @Test
    void processRequest_WithSpecialCharacters_ShouldHandleCorrectly() {
        ConnectRequestDto request = ConnectRequestDto.builder()
                .requestType(RequestType.DOCUMENT_PROCESSING)
                .payload("<document><content>Special chars: àáâãäåæçèéêë &amp; &lt; &gt; &quot; &apos;</content></document>")
                .contentType("application/xml")
                .clientId("test-client")
                .requestId("special-chars-test")
                .build();

        requestSpec
                .body(request)
                .when()
                .post("/connect/process")
                .then()
                .statusCode(202)
                .body("requestId", equalTo("special-chars-test"))
                .body("status", equalTo("ACCEPTED"));
    }

    @Test
    void processRequest_ConcurrentRequests_ShouldHandleMultipleRequests() {
        // Submit multiple requests concurrently
        for (int i = 0; i < 10; i++) {
            ConnectRequestDto request = ConnectRequestDto.builder()
                    .requestType(RequestType.GENERIC_PROCESSING)
                    .payload("<test>concurrent request " + i + "</test>")
                    .contentType("application/xml")
                    .clientId("test-client")
                    .requestId("concurrent-test-" + i)
                    .build();

            requestSpec
                    .body(request)
                    .when()
                    .post("/connect/process")
                    .then()
                    .statusCode(202)
                    .body("requestId", equalTo("concurrent-test-" + i))
                    .body("status", equalTo("ACCEPTED"));
        }
    }
}

