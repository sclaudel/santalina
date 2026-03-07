package com.example.diving.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_config")
public class AppConfigEntry extends PanacheEntityBase {

    @Id
    @Column(name = "config_key")
    public String configKey;

    @Column(name = "config_value", nullable = false)
    public String configValue;

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt = LocalDateTime.now();

    public static AppConfigEntry findByKey(String key) {
        return findById(key);
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

