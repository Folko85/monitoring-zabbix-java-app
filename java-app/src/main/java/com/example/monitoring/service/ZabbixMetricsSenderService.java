package com.example.monitoring.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.example.monitoring.dto.ZabbixObject;

import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
public class ZabbixMetricsSenderService {

    private final HealthEndpoint healthEndpoint;
    private final MeterRegistry meterRegistry;
    private final String zabbixHost = "zabbix-server"; // Имя сервиса в docker-compose
    private final int zabbixPort = 10051;
    private final String zabbixTargetHost = "java-app-host"; // Имя хоста в Zabbix


    public ZabbixMetricsSenderService(HealthEndpoint healthEndpoint, MeterRegistry meterRegistry) {
        this.healthEndpoint = healthEndpoint;
        this.meterRegistry = meterRegistry;
    }


    @Scheduled(initialDelay = 5000, fixedDelay = 5000)
    public void sendMetrics() {

        List<ZabbixObject> items = new ArrayList<>();

        getBasicItems(items);

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
                                log.info("Отправлена метрика {} :{}", item.getKey(), line);
                            }
                        }

                    } catch (IOException | InterruptedException e) {
                        log.error(Arrays.toString(e.getStackTrace()));
                    }
                }
        );
    }


    private void getBasicItems(List<ZabbixObject> items) {
        // 1. Получаем значение активных сессий из MeterRegistry
        Double activeSessionsValue = meterRegistry.get("tomcat.sessions.active.current")
                .gauge()
                .value();
        items.add(new ZabbixObject("tomcat.sessions.active.current", String.format(Locale.US, "%.2f", activeSessionsValue)));

        // 2. Получаем статус приложения.
        String status = Optional.ofNullable(healthEndpoint.health())
                .map(HealthComponent::getStatus)
                .map(Status::getCode)
                .orElse("DOWN");
        items.add(new ZabbixObject("health", status));

        // 3. Получаем значение количества загруженных классов
        Double classesLoadedValue = meterRegistry.get("jvm.classes.loaded")
                .gauge()
                .value();
        items.add(new ZabbixObject("jvm.classes.loaded", String.format(Locale.US, "%.2f", classesLoadedValue)));

        // 4. Получаем значение размера долгоживущих данных GC
        Double gcLiveDataSizeValue = meterRegistry.get("jvm.gc.live.data.size")
                .gauge()
                .value();
        items.add(new ZabbixObject("jvm.gc.live.data.size", String.format(Locale.US, "%.2f", gcLiveDataSizeValue)));

        // 5. Получаем значение используемой heap-памяти с указанием тега 'area=heap'
        Double heapUsedValue = meterRegistry
                .get("jvm.memory.used")
                .tag("area", "heap")
                .gauge()
                .value();
        items.add(new ZabbixObject("jvm.memory.used[heap]", String.format(Locale.US, "%.2f", heapUsedValue)));

        // 6. Получаем значение используемой heap-памяти с указанием тега 'area=heap'
        Double nonHeapUsedValue = meterRegistry
                .get("jvm.memory.used")
                .tag("area", "nonheap")
                .gauge()
                .value();
        items.add(new ZabbixObject("jvm.memory.used[nonheap]", String.format(Locale.US, "%.2f", nonHeapUsedValue)));

        // 7. Получаем значение количества демон-потоков
        Double daemonThreadsValue = meterRegistry
                .get("jvm.threads.daemon")
                .gauge()
                .value();
        items.add(new ZabbixObject("jvm.threads.daemon", String.format(Locale.US, "%.2f", daemonThreadsValue)));

        // 8. Получаем общее количество живых потоков
        Double liveThreadsValue = meterRegistry
                .get("jvm.threads.live")
                .gauge()
                .value();
        items.add(new ZabbixObject("jvm.threads.live", String.format(Locale.US, "%.2f", liveThreadsValue)));

        // 9. Получаем значение использования CPU процессом
        Double cpuUsageValue = meterRegistry
                .get("process.cpu.usage")
                .gauge()
                .value();
        items.add(new ZabbixObject("process.cpu.usage", String.format(Locale.US, "%.2f", cpuUsageValue)));

        // 10. Получаем значение uptime процесса
        Double uptimeValue = meterRegistry
                .get("process.uptime")
                .gauge()
                .value();
        items.add(new ZabbixObject("process.uptime", String.format(Locale.US, "%.2f", uptimeValue)));
    }

}