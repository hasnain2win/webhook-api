package com.hasnain.webhook_api;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class StreamRequest {

    private String callerId;
    private String agentId;

}
