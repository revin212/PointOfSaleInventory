package com.smartpos.backend.customers;

import com.smartpos.backend.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class CustomerEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 180)
    private String name;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "email", length = 180)
    private String email;

    @Column(name = "notes", length = 500)
    private String notes;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

