package com.example.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class ZabbixObject {

    private String key;
    private String value;

}
