package me.yuanbin.kafka.task.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import me.yuanbin.common.util.TypesafeConfigUtil;
import me.yuanbin.kafka.predicate.MaxwellKeyPredicate;
import me.yuanbin.kafka.task.AbstractTask;
import me.yuanbin.kafka.task.StreamTask;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author billryan
 * @date 2019-07-05
 */
public class MaxwellKeyRouterTask extends AbstractTask implements StreamTask {

    private static final Logger log = LoggerFactory.getLogger(MaxwellKeyRouterTask.class);

    @Override
    public void load(StreamsBuilder builder) {
        final KStream<JsonNode, byte[]> sourceStream = builder.stream(sourceTopics, jsonBytesConsumed);

        for (Map.Entry<String, ConfigValue> entry : sinkTopicRouter.entrySet()) {
            // TypesafeConfig would make xxx.xxx as a unique key, so we should split it with .
            String rawSinkTopic = entry.getKey();
            String sinkTopic = rawSinkTopic.split("\\.")[0];
            log.info("build stream task with sink.topic {}", sinkTopic);
            Config sinkTopicConfig = sinkTopicRouter.getConfig(sinkTopic);
            List<String> whitelist = TypesafeConfigUtil.getStringList(sinkTopicConfig, WHITELIST);
            boolean enableWhitelistRegex = sinkTopicConfig.hasPath(ENABLE_WHITELIST_REGEX)
                    && sinkTopicConfig.getBoolean(ENABLE_WHITELIST_REGEX);
            sourceStream
                    .filter(new MaxwellKeyPredicate(whitelist, enableWhitelistRegex))
                    .to(sinkTopic, jsonBytesProduced);
        }
    }
}
