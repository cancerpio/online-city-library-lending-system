package com.library.auth;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "external_verifications")
public class ExternalVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_no")
    private String employeeNo;

    @CreationTimestamp
    @Column(name = "verifications_time")
    private LocalDateTime verificationsTime;

    @Column(name = "verifications_result")
    private Boolean verificationsResult;

    // Constructors
    public ExternalVerification() {}

    public ExternalVerification(Long id, String employeeNo, LocalDateTime verificationsTime, Boolean verificationsResult) {
        this.id = id;
        this.employeeNo = employeeNo;
        this.verificationsTime = verificationsTime;
        this.verificationsResult = verificationsResult;
    }

    // Builder pattern
    public static ExternalVerificationBuilder builder() {
        return new ExternalVerificationBuilder();
    }

    public static class ExternalVerificationBuilder {
        private Long id;
        private String employeeNo;
        private LocalDateTime verificationsTime;
        private Boolean verificationsResult;

        public ExternalVerificationBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ExternalVerificationBuilder employeeNo(String employeeNo) {
            this.employeeNo = employeeNo;
            return this;
        }

        public ExternalVerificationBuilder verificationsTime(LocalDateTime verificationsTime) {
            this.verificationsTime = verificationsTime;
            return this;
        }

        public ExternalVerificationBuilder verificationsResult(Boolean verificationsResult) {
            this.verificationsResult = verificationsResult;
            return this;
        }

        public ExternalVerification build() {
            return new ExternalVerification(id, employeeNo, verificationsTime, verificationsResult);
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeNo() {
        return employeeNo;
    }

    public void setEmployeeNo(String employeeNo) {
        this.employeeNo = employeeNo;
    }

    public LocalDateTime getVerificationsTime() {
        return verificationsTime;
    }

    public void setVerificationsTime(LocalDateTime verificationsTime) {
        this.verificationsTime = verificationsTime;
    }

    public Boolean getVerificationsResult() {
        return verificationsResult;
    }

    public void setVerificationsResult(Boolean verificationsResult) {
        this.verificationsResult = verificationsResult;
    }
}
