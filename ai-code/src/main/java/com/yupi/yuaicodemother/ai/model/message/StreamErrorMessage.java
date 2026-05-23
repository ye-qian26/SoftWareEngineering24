package com.yupi.yuaicodemother.ai.model.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class StreamErrorMessage extends StreamMessage {

    private String stage;

    private Integer code;

    private String message;

    private Boolean retryable;

    private Boolean preservePreview;

    private Boolean fatal;

    public StreamErrorMessage(String stage, Integer code, String message,
                              Boolean retryable, Boolean preservePreview, Boolean fatal) {
        super(StreamMessageTypeEnum.STREAM_ERROR.getValue());
        this.stage = stage;
        this.code = code;
        this.message = message;
        this.retryable = retryable;
        this.preservePreview = preservePreview;
        this.fatal = fatal;
    }
}
