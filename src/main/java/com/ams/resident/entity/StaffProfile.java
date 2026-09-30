package com.ams.resident.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("STAFF")
@Getter
@Setter
public class StaffProfile extends Profile {
    private String department;
}
