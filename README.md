# jbh-api-client
gateway-client as a standalone library project.

## Usage Example for Documentation

// What microservices import - clean and minimal

```java

import users.domains.com.jbh.gateway.JbhUserApiGatewayClient;
import com.jbh.gateway.client.JbhGatewayClientBuilder;

// Simple usage
var config = new JbhGatewayClientBuilder("https://gateway.jbh.com");
    var userClient = JbhGatewayClientFactory.createUserApiClient(config);

    var headers = Map.of(
        "Authorization", "Bearer " + token,
        "JBH-XXX-REQ-SOURCE", "user-service"
    );

    var response = userClient.findUserId(headers);
```

