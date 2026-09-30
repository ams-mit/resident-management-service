package com.ams.resident.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("RESIDENT")
@Getter
@Setter
public class ResidentProfile extends Profile {
    private String emergencyContact;
}
