package com.smartpos.backend.categories;

import com.smartpos.backend.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class CategoryEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 120, unique = true)
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
