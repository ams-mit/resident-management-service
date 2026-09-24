package com.ams.resident.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "profiles")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "profile_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
public abstract class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, unique = true)
    private String userId;
    
    private String firstName;
    private String lastName;
    private String phone;
    
    @Column(name = "profile_type", insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.VARCHAR)
    private ProfileType profileType;
}
