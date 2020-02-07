/*
 * MIT License
 *
 * Copyright (c) 2019 everythingbest
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dubbo.postman.config;

import com.dubbo.postman.util.Constant;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author everythingbest
 * redis连接相关的配置
 */
@Configuration
public class RedisConfig {

    @Value("${sentinel.master}")
    String nodeMaster;

    @Value("${redis.password}")
    String nodePassword;

    @Value("${node1.ip}")
    String node1Ip;

    @Value("${node2.ip}")
    String node2Ip;

    @Value("${node3.ip}")
    String node3Ip;

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {

        String[] node1 = node1Ip.split(Constant.PORT_SPLITTER);
        String[] node2 = node1Ip.split(Constant.PORT_SPLITTER);
        String[] node3 = node1Ip.split(Constant.PORT_SPLITTER);

        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
                .master(nodeMaster)
                .sentinel(node1[0], Integer.valueOf(node1[1]))
                .sentinel(node2[0], Integer.valueOf(node2[1]))
                .sentinel(node3[0], Integer.valueOf(node3[1]));

        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(sentinelConfig);

//        jedisConnectionFactory.setPassword(nodePassword);

        JedisPoolConfig poolConfig = new JedisPoolConfig();

        poolConfig.setTestOnBorrow(true);

        jedisConnectionFactory.setPoolConfig(poolConfig);

        return jedisConnectionFactory;
    }

    // 解决使用redisTemplate存储数据,key前出现\xAC\xED\x00\x05t\x00
    @Bean
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 使用Jackson2JsonRedisSerialize 替换默认序列化
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);

        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

        // 设置value的序列化规则和 key的序列化规则
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        /*hash字符串序列化方法*/
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
