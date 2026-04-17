package com.smartpos.backend.suppliers;

import com.smartpos.backend.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "suppliers")
public class SupplierEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 180)
    private String name;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "address", length = 500)
    private String address;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
