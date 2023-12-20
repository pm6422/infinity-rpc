package com.luixtech.rpc.democommon.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Set;

/**
 * Spring Data MongoDB collection for the App entity.
 */
@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
public class App implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(required = true)
    @NotNull
    @Size(min = 3, max = 20)
    @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "{EP5903}")
    @Id
    private String name;

    private Boolean enabled;

    @Transient
    private Set<String> authorities;

    public App(String name, Boolean enabled) {
        this.name = name;
        this.enabled = enabled;
    }
}
