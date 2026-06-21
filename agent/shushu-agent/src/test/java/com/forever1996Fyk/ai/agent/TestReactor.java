package com.forever1996Fyk.ai.agent;

import org.apache.commons.lang3.ThreadUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/21 22:24
 **/
public class TestReactor {

    public static void main(String[] args) throws InterruptedException {
        testFlatmap();

        System.out.println("hello");

        ThreadUtils.sleep(Duration.ofMillis(5000));
    }

    private static void testMono1() {
        Mono<String> mono = Mono.just("你好");
        mono.subscribe(res -> {
            System.out.println(res);
        });
    }

    private static void testMono2() {
        // 异步执行Mono
        Mono<String> mono = Mono.just("你好")
                        .subscribeOn(Schedulers.boundedElastic());
        mono.subscribe(res -> {
            System.out.println(res);
        });
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void testMono3() {
        Mono<String> mono = Mono.fromSupplier(() -> getData());
        mono.subscribe(res -> {
            System.out.println(res);
        });
    }

    private static void testMono4() {
        Mono<Object> mono = Mono.fromRunnable(() -> {
            getData();
        });

        mono.subscribe(res -> {
            System.out.println(res);
        });
    }

    private static String getData() {
        System.out.println("getData");
        return "hello";
    }

    private static void testFlux1() {
        Flux<String> just = Flux.just("he", "ll", "o")
                // 延迟输出
                .delayElements(Duration.ofSeconds(1));
        just.subscribe(res -> {
            System.out.println(res);
        });
    }

    private static void testFlux2() {
        Flux<Object> flux = Flux.create(fluxSink -> {
            for (int i = 0; i < 10; i++) {
                // 输出发送
                fluxSink.next(i);
            }
            fluxSink.complete();
        });

        // 数据订阅
        flux.subscribe(res -> {
            System.out.println(res);
        });
    }

    private static void testSinks1() {
        // many: 可以发送多个数据
        // unicast: 只会有一个流式输出的地址
        // onBackpressureBuffer: 背压，相当于一个缓冲区，把发送到数据，发送到缓冲区，防止消费速度与生产速度不对等，导致内存持续占用，即内容溢出
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        // 把sink转换为Flux流，这样前端就可以订阅Flux获取数据
        Flux<String> flux = sink.asFlux();

        Disposable disposable = flux.subscribe(res -> {
            System.out.println(res);
        });

        // 使用sink发送数据
        sink.tryEmitNext("你好");
        sink.tryEmitNext("我是");

        disposable.dispose();

        sink.tryEmitNext("澍澍");

        // 结束发送
        sink.tryEmitComplete();
    }

    private static void testFlatmap() {
        Flux<String> flux = Flux.just("问题1", "问题2");

        Flux<String> result = flux.flatMap(question -> {
            return getData(question);
        });

        result.subscribe(res -> {
            System.out.println(res);
        });
    }

    private static Mono<String> getData(String q) {
        return Mono.just(q + " answer");
    }
}
