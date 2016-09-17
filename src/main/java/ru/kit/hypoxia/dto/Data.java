package ru.kit.hypoxia.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Inspection.class, name="Inspection"),
        @JsonSubTypes.Type(value = Inspections.class, name="Inspections"),
        @JsonSubTypes.Type(value = LastResearch.class, name="LastResearch"),
        @JsonSubTypes.Type(value = ReadyStatus.class, name="ReadyStatus"),
})
public abstract class Data {
}
