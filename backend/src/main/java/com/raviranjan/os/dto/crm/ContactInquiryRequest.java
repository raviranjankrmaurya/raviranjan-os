package com.raviranjan.os.dto.crm;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ContactInquiryRequest {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 80, message = "Name must be between 2 and 80 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    private String email;

    @Size(max = 150)
    private String company;

    @Size(max = 255)
    private String subject;

    @NotBlank(message = "Message content is required")
    @Size(min = 2, max = 5000, message = "Message must be between 2 and 5000 characters")
    private String message;

    @Size(max = 128)
    private String browserIdentifier;

    private String browserHash;
    private String clientIp;
    private String city;
    private String region;
    private String country;
    private String origin;
    private String referrer;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getBrowserIdentifier() { return browserIdentifier; }
    public void setBrowserIdentifier(String browserIdentifier) { this.browserIdentifier = browserIdentifier; }

    public String getBrowserHash() { return browserHash; }
    public void setBrowserHash(String browserHash) { this.browserHash = browserHash; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getReferrer() { return referrer; }
    public void setReferrer(String referrer) { this.referrer = referrer; }
}
