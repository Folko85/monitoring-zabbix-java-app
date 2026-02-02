package com.example.monitoring.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.example.monitoring.dto.ZabbixObject;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
public class ZabbixMetricsSenderService {

    private final MeterRegistry meterRegistry;
    private final String zabbixHost = "zabbix-server"; // Имя сервиса в docker-compose
    private final int zabbixPort = 10051;
    private final String zabbixTargetHost = "java-app-host"; // Имя хоста в Zabbix


    public ZabbixMetricsSenderService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }


    @Scheduled(fixedDelay = 3000) // Отправка каждые 3 секунды
    public void sendMetrics() {

        List<ZabbixObject> items = new ArrayList<>();

        // 1. Сбор метрик из Actuator (Micrometer)
        // Пример: Использование heap-памяти
        Double heapUsed = meterRegistry.get("jvm.memory.used")
                .tag("area", "heap")
                .gauge()
                .value();
        items.add(new ZabbixObject("jvm.memory.heap.used", String.format("%.6f", heapUsed)));
        log.info("Метрика jvm.memory.used равна {}", heapUsed);

        // Пример: Количество активных потоков
        Double threadCount = meterRegistry.get("jvm.threads.live")
                .gauge()
                .value();
        items.add(new ZabbixObject("jvm.threads.live", String.format("%.6f", threadCount)));
        log.info("Метрика jvm.threads.live равна {}", threadCount);

        items.forEach(item ->
                {
                    Process process;
                    String command = String.format("zabbix_sender -z %s -p %d -s %s -k %s -o %s", zabbixHost, zabbixPort, zabbixTargetHost, item.getKey(),
                            item.getValue());
                    try {
                        process = Runtime.getRuntime()
                                .exec(command);
                        BufferedReader response = new BufferedReader(new InputStreamReader(process.getInputStream()));

                        if (response.readLine() == null) {
                            process.waitFor();
                        } else {
                            String line;
                            while ((line = response.readLine()) != null) {
                                log.info(line);
                            }
                        }

                    } catch (IOException | InterruptedException e) {
                        log.error(Arrays.toString(e.getStackTrace()));
                    }
                }
        );
    }

}
