package com.ams.resident.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("OWNER")
@Getter
@Setter
public class OwnerProfile extends Profile {
    private String taxId;
}
